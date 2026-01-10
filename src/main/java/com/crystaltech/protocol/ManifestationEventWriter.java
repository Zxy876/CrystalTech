package com.crystaltech.protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.crystaltech.CrystalTech;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Append-only writer for manifestation event telemetry consumed by the Ideal City plugin.
 */
public final class ManifestationEventWriter {
    private static final Gson GSON = new Gson();

    private final Path logFile;
    private final Lock lock = new ReentrantLock();
    private final boolean enabled;

    private ManifestationEventWriter(Path logFile, boolean enabled) {
        this.logFile = logFile;
        this.enabled = enabled;
    }

    public static ManifestationEventWriter disabled() {
        return new ManifestationEventWriter(null, false);
    }

    public static ManifestationEventWriter forFile(Path logFile) {
        return new ManifestationEventWriter(logFile, true);
    }

    public void recordIntentAccepted(UUID playerId, ManifestationIntent intent, String signatureStatus, Instant timestamp) {
        if (!enabled) {
            return;
        }
        JsonObject object = baseIntentObject("intent_accepted", playerId, intent, timestamp);
        object.addProperty("signature_status", signatureStatus);
        write(object);
    }

    public void recordIntentRejected(UUID playerId, ManifestationIntent intent, String reason, String signatureStatus, Instant timestamp) {
        if (!enabled) {
            return;
        }
        JsonObject object = baseIntentObject("intent_rejected", playerId, intent, timestamp);
        object.addProperty("reason", reason);
        object.addProperty("signature_status", signatureStatus);
        write(object);
    }

    public void recordIntentIgnored(UUID playerId, ManifestationIntent intent, String reason, Instant timestamp) {
        if (!enabled) {
            return;
        }
        JsonObject object = baseIntentObject("intent_ignored", playerId, intent, timestamp);
        object.addProperty("reason", reason);
        write(object);
    }

    public void recordStageManifested(UUID playerId,
                                      String playerName,
                                      String intentId,
                                      String scenarioId,
                                      String scenarioVersion,
                                      int stage,
                                      Instant timestamp) {
        if (!enabled) {
            return;
        }
        JsonObject object = new JsonObject();
        object.addProperty("timestamp", timestamp.toString());
        object.addProperty("event", "stage_manifested");
        object.addProperty("player_id", playerId.toString());
        if (playerName != null) {
            object.addProperty("player_name", playerName);
        }
        if (intentId != null) {
            object.addProperty("intent_id", intentId);
        }
        if (scenarioId != null) {
            object.addProperty("scenario_id", scenarioId);
        }
        if (scenarioVersion != null) {
            object.addProperty("scenario_version", scenarioVersion);
        }
        object.addProperty("stage", stage);
        write(object);
    }

    private JsonObject baseIntentObject(String type, UUID playerId, ManifestationIntent intent, Instant timestamp) {
        JsonObject object = new JsonObject();
        object.addProperty("timestamp", timestamp.toString());
        object.addProperty("event", type);
        object.addProperty("player_id", playerId.toString());
        object.addProperty("intent_id", intent.intentId());
        object.addProperty("schema_version", intent.schemaVersion());
        object.addProperty("scenario_id", intent.scenarioId());
        if (intent.scenarioVersion() != null) {
            object.addProperty("scenario_version", intent.scenarioVersion());
        }
        object.addProperty("allowed_stage", intent.allowedStage());
        object.addProperty("confidence_level", intent.confidenceLevel());
        object.addProperty("issued_at", intent.issuedAt().toString());
        if (intent.expiresAt() != null) {
            object.addProperty("expires_at", intent.expiresAt().toString());
        }
        if (!intent.constraints().isEmpty()) {
            object.add("constraints", toArray(intent.constraints()));
        }
        if (!intent.contextNotes().isEmpty()) {
            object.add("context_notes", toArray(intent.contextNotes()));
        }
        return object;
    }

    private JsonArray toArray(List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private void write(JsonObject object) {
        lock.lock();
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            Files.writeString(logFile,
                    GSON.toJson(object) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ex) {
            CrystalTech.LOGGER.error("Failed to append manifestation event log {}", logFile, ex);
        } finally {
            lock.unlock();
        }
    }
}
