package com.crystaltech.protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.crystaltech.CrystalTech;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * File-based manifestation intent inbox. The Ideal City plugin drops JSON envelopes into
 * {@code pending/}, Forge consumes them, moves them to {@code processing/} while in-flight, and
 * finally to {@code processed/} once acknowledged. Invalid payloads are moved to {@code failed/}.
 */
public final class FileManifestationIntentInbox implements ManifestationIntentInbox {
    private static final String ROOT_PLAYER_ID = "player_id";
    private static final String ROOT_INTENT = "intent";

    private final Path pendingDir;
    private final Path processingDir;
    private final Path processedDir;
    private final Path failedDir;
    private final Map<String, Path> inFlight = new ConcurrentHashMap<>();

    public FileManifestationIntentInbox(Path rootDirectory) {
        this.pendingDir = rootDirectory.resolve("pending");
        this.processingDir = rootDirectory.resolve("processing");
        this.processedDir = rootDirectory.resolve("processed");
        this.failedDir = rootDirectory.resolve("failed");
        try {
            Files.createDirectories(pendingDir);
            Files.createDirectories(processingDir);
            Files.createDirectories(processedDir);
            Files.createDirectories(failedDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialise intent inbox directories", ex);
        }
    }

    @Override
    public Optional<Message> poll() {
        Path nextFile = findNextPendingFile();
        if (nextFile == null) {
            return Optional.empty();
        }

        Path processingFile = processingDir.resolve(nextFile.getFileName());
        try {
            Files.move(nextFile, processingFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            CrystalTech.LOGGER.error("Failed to move intent file {} into processing state", nextFile, ex);
            moveToFailed(nextFile, "move_to_processing_failed");
            return Optional.empty();
        }

        try {
            String raw = Files.readString(processingFile, StandardCharsets.UTF_8);
            JsonObject envelope = JsonParser.parseString(raw).getAsJsonObject();
            UUID playerId = UUID.fromString(requiredString(envelope, ROOT_PLAYER_ID));
            JsonElement intentElement = envelope.get(ROOT_INTENT);
            if (intentElement == null || !intentElement.isJsonObject()) {
                throw new JsonParseException("Missing intent object");
            }
            String rawIntentJson = intentElement.toString();
            ManifestationIntent intent = ManifestationIntentCodec.decode(rawIntentJson);
            inFlight.put(intent.intentId(), processingFile);
            return Optional.of(new Message(playerId, intent, rawIntentJson));
        } catch (Exception ex) {
            CrystalTech.LOGGER.error("Failed to parse manifestation intent from file {}", processingFile, ex);
            moveToFailed(processingFile, "parse_failed");
            return Optional.empty();
        }
    }

    @Override
    public void acknowledge(String intentId) {
        Path file = inFlight.remove(intentId);
        if (file == null) {
            CrystalTech.LOGGER.warn("Attempted to acknowledge unknown intent {}", intentId);
            return;
        }
        Path target = processedDir.resolve(file.getFileName());
        try {
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            CrystalTech.LOGGER.error("Failed to move processed intent {} to archive", file, ex);
        }
    }

    @Override
    public void reject(String intentId, String reason) {
        Path file = inFlight.remove(intentId);
        if (file == null) {
            CrystalTech.LOGGER.warn("Attempted to reject unknown intent {}", intentId);
            return;
        }
        moveToFailed(file, reason == null ? "rejected" : reason);
    }

    private Path findNextPendingFile() {
        try (Stream<Path> stream = Files.list(pendingDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(this::safeFileTime).thenComparing(Path::getFileName))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ex) {
            CrystalTech.LOGGER.error("Failed to scan intent pending directory {}", pendingDir, ex);
            return null;
        }
    }

    private FileTime safeFileTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException ex) {
            return FileTime.fromMillis(0L);
        }
    }

    private void moveToFailed(Path source, String reason) {
        Path file = source;
        if (!source.getParent().equals(failedDir)) {
            Path target = failedDir.resolve(source.getFileName());
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                file = target;
            } catch (IOException ex) {
                CrystalTech.LOGGER.error("Failed to move intent file {} to failed directory", source, ex);
                return;
            }
        }
        try {
            Path reasonFile = file.resolveSibling(file.getFileName().toString() + ".reason");
            Files.writeString(reasonFile, (reason == null ? "unspecified" : reason) + " @ " + Instant.now(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            CrystalTech.LOGGER.warn("Failed to record failure reason for {}", file, ex);
        }
    }

    private static String requiredString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null) {
            throw new JsonParseException("Missing field: " + key);
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Field " + key + " must be a string");
        }
        return element.getAsString();
    }
}
