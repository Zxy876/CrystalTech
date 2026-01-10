package com.crystaltech.protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.crystaltech.CrystalTech;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Handles generation of CityPhone social feed artefacts and trust index updates.
 */
public final class SocialFeedWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")
            .withZone(ZoneOffset.UTC);
    private static final double DEFAULT_TRUST_INDEX = 0.5d;
    private static final double STAGE_ADVANCE_DELTA = 0.05d;

    private final Path feedDirectory;
    private final Path trustIndexFile;
    private final Lock lock = new ReentrantLock();
    private final boolean enabled;
    private final Path eventsLogFile;

    private double trustIndex = Double.NaN;

    private SocialFeedWriter(Path feedDirectory, Path trustIndexFile, Path eventsLogFile, boolean enabled) {
        this.feedDirectory = feedDirectory;
        this.trustIndexFile = trustIndexFile;
        this.eventsLogFile = eventsLogFile;
        this.enabled = enabled;
    }

    public static SocialFeedWriter disabled() {
        return new SocialFeedWriter(null, null, null, false);
    }

    public static SocialFeedWriter forDirectory(Path feedDirectory) {
        Path trustFile = feedDirectory.getParent() != null ? feedDirectory.getParent().resolve("trust_index.json")
                : feedDirectory.resolve("trust_index.json");
        Path eventsLog = feedDirectory.resolve("events.jsonl");
        return new SocialFeedWriter(feedDirectory, trustFile, eventsLog, true);
    }

    public void recordStageAdvance(UUID playerId,
                                   String playerName,
                                   String scenarioId,
                                   String scenarioVersion,
                                   int stage,
                                   Instant timestamp) {
        if (!enabled) {
            return;
        }
        lock.lock();
        try {
            ensureDirectories();
            double updatedTrust = adjustTrustIndex(STAGE_ADVANCE_DELTA);
            String fileName = FILENAME_FORMATTER.format(timestamp) + "-stage-" + stage + ".json";
            Path target = feedDirectory.resolve(fileName);
            JsonObject entry = new JsonObject();
            entry.addProperty("timestamp", timestamp.toString());
            entry.addProperty("event_type", "stage_advance");
            entry.addProperty("stage", stage);
            if (playerId != null) {
                entry.addProperty("player_id", playerId.toString());
            }
            if (playerName != null) {
                entry.addProperty("player_name", playerName);
            }
            if (scenarioId != null) {
                entry.addProperty("scenario_id", scenarioId);
            }
            if (scenarioVersion != null) {
                entry.addProperty("scenario_version", scenarioVersion);
            }
            entry.addProperty("trust_index", updatedTrust);
            writeAtomically(target, GSON.toJson(entry));
            appendEvent(entry);
            writeTrustIndex(timestamp, updatedTrust);
        } catch (IOException ex) {
            CrystalTech.LOGGER.error("Failed to write social feed entry", ex);
        } finally {
            lock.unlock();
        }
    }

    private void ensureDirectories() throws IOException {
        Objects.requireNonNull(feedDirectory, "feedDirectory");
        Files.createDirectories(feedDirectory);
    }

    private double adjustTrustIndex(double delta) throws IOException {
        double current = loadTrustIndex();
        double updated = clamp(current + delta, 0.0d, 1.0d);
        trustIndex = updated;
        return updated;
    }

    private double loadTrustIndex() throws IOException {
        if (!Double.isNaN(trustIndex)) {
            return trustIndex;
        }
        if (trustIndexFile != null && Files.exists(trustIndexFile)) {
            try {
                String raw = Files.readString(trustIndexFile, StandardCharsets.UTF_8);
                JsonObject object = GSON.fromJson(raw, JsonObject.class);
                if (object != null && object.has("value")) {
                    trustIndex = clamp(object.get("value").getAsDouble(), 0.0d, 1.0d);
                    return trustIndex;
                }
            } catch (Exception ex) {
                CrystalTech.LOGGER.warn("Failed to parse existing trust index file {}", trustIndexFile, ex);
            }
        }
        trustIndex = DEFAULT_TRUST_INDEX;
        return trustIndex;
    }

    private void writeTrustIndex(Instant timestamp, double value) throws IOException {
        if (trustIndexFile == null) {
            return;
        }
        JsonObject object = new JsonObject();
        object.addProperty("timestamp", timestamp.toString());
        object.addProperty("value", value);
        writeAtomically(trustIndexFile, GSON.toJson(object));
    }

    private void appendEvent(JsonObject entry) throws IOException {
        if (eventsLogFile == null) {
            return;
        }
        Path parent = eventsLogFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String payload = GSON.toJson(entry) + System.lineSeparator();
        Files.writeString(eventsLogFile, payload, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void writeAtomically(Path target, String contents) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, contents, StandardCharsets.UTF_8);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
