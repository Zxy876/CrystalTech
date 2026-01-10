package com.crystaltech.protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.crystaltech.CrystalTech;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * Maintains the shared technology status envelope consumed by the CityPhone plugin.
 */
public final class TechnologyStatusWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_RECENT_EVENTS = 32;

    private final Path statusFile;
    private final Lock lock = new ReentrantLock();
    private final boolean enabled;

    private TechnologyStatus snapshot;

    private TechnologyStatusWriter(Path statusFile, boolean enabled) {
        this.statusFile = statusFile;
        this.enabled = enabled;
    }

    public static TechnologyStatusWriter disabled() {
        return new TechnologyStatusWriter(null, false);
    }

    public static TechnologyStatusWriter forFile(Path statusFile) {
        return new TechnologyStatusWriter(statusFile, true);
    }

    public void updateStage(int stage,
                            String scenarioId,
                            String scenarioVersion,
                            Instant timestamp) {
        if (!enabled) {
            return;
        }
        lock.lock();
        try {
            TechnologyStatus current = ensureSnapshot();
            snapshot = current.withStage(stage, scenarioId, scenarioVersion, timestamp);
            writeSnapshot(snapshot);
        } catch (IOException ex) {
            CrystalTech.LOGGER.error("Failed to write technology status", ex);
        } finally {
            lock.unlock();
        }
    }

    public void updateEnergy(String status,
                             int level,
                             Double generation,
                             Double consumption,
                             Double reserve,
                             Instant timestamp) {
        if (!enabled) {
            return;
        }
        lock.lock();
        try {
            TechnologyStatus current = ensureSnapshot();
            snapshot = current.withEnergy(Energy.fromSnapshot(status, level, generation, consumption, reserve, timestamp), timestamp);
            writeSnapshot(snapshot);
        } catch (IOException ex) {
            CrystalTech.LOGGER.error("Failed to write technology energy snapshot", ex);
        } finally {
            lock.unlock();
        }
    }

    public void replaceRisks(List<RiskEntry> riskEntries, Instant timestamp) {
        if (!enabled) {
            return;
        }
        lock.lock();
        try {
            TechnologyStatus current = ensureSnapshot();
            snapshot = current.withRisks(riskEntries, timestamp);
            writeSnapshot(snapshot);
        } catch (IOException ex) {
            CrystalTech.LOGGER.error("Failed to write technology risks", ex);
        } finally {
            lock.unlock();
        }
    }

    public void recordStageManifest(UUID playerId,
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
            TechnologyStatus current = ensureSnapshot();
            snapshot = current.withEvent(RecentEvent.stageManifest(playerId, playerName, scenarioId, scenarioVersion, stage, timestamp), MAX_RECENT_EVENTS, timestamp);
            writeSnapshot(snapshot);
        } catch (IOException ex) {
            CrystalTech.LOGGER.error("Failed to append technology status event", ex);
        } finally {
            lock.unlock();
        }
    }

    private TechnologyStatus ensureSnapshot() {
        if (snapshot != null) {
            return snapshot;
        }
        if (statusFile != null && Files.exists(statusFile)) {
            try {
                String raw = Files.readString(statusFile, StandardCharsets.UTF_8);
                TechnologyStatus parsed = GSON.fromJson(raw, TechnologyStatus.class);
                if (parsed != null) {
                    snapshot = parsed.ensureDefaults(Instant.now());
                    return snapshot;
                }
            } catch (Exception ex) {
                CrystalTech.LOGGER.warn("Failed to parse technology status file {}", statusFile, ex);
            }
        }
        snapshot = TechnologyStatus.initial(Instant.now());
        return snapshot;
    }

    private void writeSnapshot(TechnologyStatus status) throws IOException {
        if (statusFile == null) {
            return;
        }
        Path parent = statusFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = statusFile.resolveSibling(statusFile.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(status), StandardCharsets.UTF_8);
        Files.move(tmp, statusFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * JSON-mapped snapshot object.
     */
    private static final class TechnologyStatus {
        private String timestamp;
        private String updated_at;
        private Stage stage;
        private Energy energy;
        private List<RiskEntry> risks;
        private List<String> alerts;
        @SerializedName("recent_events")
        private List<RecentEvent> recentEvents;

        TechnologyStatus(Instant timestamp,
                         Stage stage,
                         Energy energy,
                         List<RiskEntry> risks,
                         List<String> alerts,
                         List<RecentEvent> recentEvents) {
            Instant safeTimestamp = timestamp == null ? Instant.now() : timestamp;
            this.timestamp = safeTimestamp.toString();
            this.updated_at = safeTimestamp.toString();
            this.stage = stage != null ? stage : Stage.initial(safeTimestamp);
            this.energy = energy != null ? energy : Energy.initial(safeTimestamp);
            this.risks = risks != null ? new ArrayList<>(risks) : new ArrayList<>();
            this.alerts = alerts != null ? new ArrayList<>(alerts) : new ArrayList<>();
            this.recentEvents = recentEvents != null ? new ArrayList<>(recentEvents) : new ArrayList<>();
        }

        TechnologyStatus ensureDefaults(Instant now) {
            Instant safeNow = now == null ? Instant.now() : now;
            if (timestamp == null) {
                timestamp = safeNow.toString();
            }
            if (updated_at == null) {
                updated_at = safeNow.toString();
            }
            if (stage == null) {
                stage = Stage.initial(safeNow);
            }
            if (energy == null) {
                energy = Energy.initial(safeNow);
            }
            if (risks == null) {
                risks = new ArrayList<>();
            }
            if (alerts == null) {
                alerts = new ArrayList<>();
            }
            if (recentEvents == null) {
                recentEvents = new ArrayList<>();
            }
            return this;
        }

        static TechnologyStatus initial(Instant now) {
            return new TechnologyStatus(now, Stage.initial(now), Energy.initial(now), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        TechnologyStatus withStage(int stageValue,
                                   String scenarioId,
                                   String scenarioVersion,
                                   Instant updatedAt) {
            Stage baseStage = stage != null ? stage : Stage.initial(updatedAt);
            Stage updatedStage = baseStage.update(stageValue, scenarioId, scenarioVersion, updatedAt);
            Energy baseEnergy = energy != null ? energy : Energy.initial(updatedAt);
            Energy reconciledEnergy = baseEnergy.reconcileForStage(stageValue, updatedAt);
            return new TechnologyStatus(updatedAt, updatedStage, reconciledEnergy, copyRisks(risks), copyAlerts(alerts), copyEvents(recentEvents));
        }

        TechnologyStatus withEnergy(Energy newEnergy, Instant updatedAt) {
            Stage refreshedStage = stage != null ? stage.refresh(updatedAt) : Stage.initial(updatedAt);
            return new TechnologyStatus(updatedAt, refreshedStage, newEnergy, copyRisks(risks), copyAlerts(alerts), copyEvents(recentEvents));
        }

        TechnologyStatus withRisks(List<RiskEntry> newRisks, Instant updatedAt) {
            Stage refreshedStage = stage != null ? stage.refresh(updatedAt) : Stage.initial(updatedAt);
            Energy refreshedEnergy = energy != null ? energy.refresh(updatedAt) : Energy.initial(updatedAt);
            return new TechnologyStatus(updatedAt, refreshedStage, refreshedEnergy, copyRisks(newRisks), copyAlerts(alerts), copyEvents(recentEvents));
        }

        TechnologyStatus withEvent(RecentEvent event, int maxEvents, Instant updatedAt) {
            Stage refreshedStage = stage != null ? stage.refresh(updatedAt) : Stage.initial(updatedAt);
            Energy refreshedEnergy = energy != null ? energy.refresh(updatedAt) : Energy.initial(updatedAt);
            List<RecentEvent> nextEvents = copyEvents(recentEvents);
            nextEvents.add(event);
            while (nextEvents.size() > maxEvents) {
                nextEvents.remove(0);
            }
            return new TechnologyStatus(updatedAt, refreshedStage, refreshedEnergy, copyRisks(risks), copyAlerts(alerts), nextEvents);
        }

        private static List<RiskEntry> copyRisks(List<RiskEntry> source) {
            return source == null ? new ArrayList<>() : new ArrayList<>(source);
        }

        private static List<String> copyAlerts(List<String> source) {
            return source == null ? new ArrayList<>() : new ArrayList<>(source);
        }

        private static List<RecentEvent> copyEvents(List<RecentEvent> source) {
            return source == null ? new ArrayList<>() : new ArrayList<>(source);
        }
    }

    private static final class Stage {
        private int current;
        private int level;
        private String label;
        @SerializedName("scenario_id")
        private String scenarioId;
        @SerializedName("scenario_version")
        private String scenarioVersion;
        @SerializedName("updated_at")
        private String updatedAt;
        private String source;

        Stage(int current,
              int level,
              String label,
              String scenarioId,
              String scenarioVersion,
              Instant updatedAt,
              String source) {
            this.current = current;
            this.level = level;
            this.label = label;
            this.scenarioId = scenarioId;
            this.scenarioVersion = scenarioVersion;
            this.updatedAt = updatedAt.toString();
            this.source = source;
        }

        static Stage initial(Instant now) {
            return new Stage(0, 0, stageLabel(0), null, null, now, "unknown");
        }

        Stage update(int stage, String scenarioId, String scenarioVersion, Instant updatedAt) {
            return new Stage(stage, stage, stageLabel(stage), scenarioId, scenarioVersion, updatedAt, "protocol");
        }

        Stage refresh(Instant updatedAt) {
            return new Stage(current, level, label, scenarioId, scenarioVersion, updatedAt, source);
        }
    }

    private static final class Energy {
        private String status;
        private int level;
        @SerializedName("updated_at")
        private String updatedAt;
        private Double generation;
        private Double consumption;
        private Double reserve;
        private transient boolean placeholder;

        Energy(String status,
               int level,
               Instant updatedAt,
               Double generation,
               Double consumption,
               Double reserve,
               boolean placeholder) {
            this.status = (status == null || status.isBlank()) ? "unknown" : status;
            this.level = Math.max(0, Math.min(100, level));
            this.updatedAt = updatedAt.toString();
            this.generation = generation;
            this.consumption = consumption;
            this.reserve = reserve;
            this.placeholder = placeholder;
        }

        static Energy initial(Instant now) {
            return new Energy("unknown", 0, now, null, null, null, true);
        }

        static Energy fromSnapshot(String status,
                                   int level,
                                   Double generation,
                                   Double consumption,
                                   Double reserve,
                                   Instant updatedAt) {
            return new Energy(status, level, updatedAt, generation, consumption, reserve, false);
        }

        static Energy derivedForStage(int stage, Instant updatedAt) {
            int derivedLevel = Math.max(5, Math.min(100, stage * 25));
            String derivedStatus = switch (stage) {
                case 0 -> "dormant";
                case 1 -> "stable";
                case 2 -> "growing";
                default -> "expanding";
            };
            double generation = roundOneDecimal(32.0 + stage * 37.5);
            double consumption = roundOneDecimal(Math.max(18.0, generation * 0.78));
            double reserve = roundOneDecimal(Math.max(4.0, generation - consumption));
            return new Energy(derivedStatus, derivedLevel, updatedAt, generation, consumption, reserve, true);
        }

        Energy reconcileForStage(int stage, Instant updatedAt) {
            if (!placeholder && !isUnknown()) {
                return refresh(updatedAt);
            }
            return derivedForStage(stage, updatedAt);
        }

        Energy refresh(Instant updatedAt) {
            return new Energy(status, level, updatedAt, generation, consumption, reserve, placeholder);
        }

        private boolean isUnknown() {
            return status == null || status.isBlank() || "unknown".equalsIgnoreCase(status);
        }

        private static double roundOneDecimal(double value) {
            return Math.round(value * 10.0) / 10.0;
        }
    }

    public static final class RiskEntry {
        private final String id;
        private final String label;
        private final String severity;
        private final String description;
        @SerializedName("updated_at")
        private final String updatedAt;

        private RiskEntry(String id, String label, String severity, String description, Instant updatedAt) {
            this.id = id;
            this.label = label;
            this.severity = severity;
            this.description = description;
            this.updatedAt = updatedAt != null ? updatedAt.toString() : null;
        }

        public static RiskEntry of(String id, String label, String severity, String description, Instant updatedAt) {
            return new RiskEntry(id, label, severity, description, updatedAt);
        }

        public RiskEntry withUpdatedAt(Instant updatedAt) {
            return new RiskEntry(id, label, severity, description, updatedAt);
        }
    }

    private static final class RecentEvent {
        private String type;
        private Integer stage;
        private String label;
        @SerializedName("scenario_id")
        private String scenarioId;
        @SerializedName("scenario_version")
        private String scenarioVersion;
        @SerializedName("player_id")
        private String playerId;
        @SerializedName("player_name")
        private String playerName;
        private String summary;
        @SerializedName("occurred_at")
        private String occurredAt;

        RecentEvent(String type,
                    Integer stage,
                    String label,
                    String scenarioId,
                    String scenarioVersion,
                    String playerId,
                    String playerName,
                    String summary,
                    Instant occurredAt) {
            this.type = type;
            this.stage = stage;
            this.label = label;
            this.scenarioId = scenarioId;
            this.scenarioVersion = scenarioVersion;
            this.playerId = playerId;
            this.playerName = playerName;
            this.summary = summary;
            this.occurredAt = occurredAt.toString();
        }

        static RecentEvent stageManifest(UUID playerId,
                                         String playerName,
                                         String scenarioId,
                                         String scenarioVersion,
                                         int stage,
                                         Instant occurredAt) {
            String label = stageLabel(stage);
            String summary = "Stage " + stage + " manifested" + (playerName != null ? " by " + playerName : "");
            return new RecentEvent(
                    "stage_manifested",
                    stage,
                    label,
                    scenarioId,
                    scenarioVersion,
                    playerId != null ? playerId.toString() : null,
                    playerName,
                    summary,
                    occurredAt);
        }
    }

    private static String stageLabel(int stage) {
        return switch (stage) {
            case 0 -> "baseline";
            case 1 -> "materialization";
            case 2 -> "stabilization";
            default -> "stage-" + stage;
        };
    }
}
