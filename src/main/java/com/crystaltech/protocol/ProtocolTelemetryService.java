package com.crystaltech.protocol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.crystaltech.CrystalTech;
import com.crystaltech.core.CrystalStageApi;
import com.crystaltech.protocol.TechnologyStatusWriter.RiskEntry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Aggregates lightweight operational telemetry so technology-status.json carries
 * production data instead of placeholder values.
 */
public final class ProtocolTelemetryService {
    private static final ProtocolTelemetryService INSTANCE = new ProtocolTelemetryService();
    private static final long UPDATE_INTERVAL_TICKS = 20L * 30L; // update roughly every 30 seconds

    private volatile TechnologyStatusWriter statusWriter = TechnologyStatusWriter.disabled();
    private volatile Path pendingDir;
    private long lastUpdateTick = Long.MIN_VALUE;

    private ProtocolTelemetryService() {
    }

    public static ProtocolTelemetryService getInstance() {
        return INSTANCE;
    }

    public void configure(TechnologyStatusWriter writer, Path inboxRoot) {
        statusWriter = Objects.requireNonNull(writer);
        pendingDir = Objects.requireNonNull(inboxRoot).resolve("pending");
    }

    public void tick(MinecraftServer server, long gameTick, Instant now) {
        if (statusWriter == null || pendingDir == null) {
            return;
        }
        if (lastUpdateTick != Long.MIN_VALUE && gameTick - lastUpdateTick < UPDATE_INTERVAL_TICKS) {
            return;
        }
        lastUpdateTick = gameTick;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int playerCount = players.size();
        int maxStage = 0;
        int stageSum = 0;
        for (ServerPlayer player : players) {
            int stage = CrystalStageApi.getStage(player);
            stageSum += stage;
            if (stage > maxStage) {
                maxStage = stage;
            }
        }
        double averageStage = playerCount == 0 ? maxStage : (double) stageSum / playerCount;

        long dayTime = server.overworld().getDayTime() % 24000L;
        double timeFactor = Math.sin((dayTime / 24000.0) * Math.PI * 2.0);

        int energyLevel = (int) Math.round(clamp(40.0 + averageStage * 25.0 + playerCount * 7.5 + timeFactor * 12.5, 5.0, 100.0));
        String energyStatus;
        if (energyLevel >= 70) {
            energyStatus = "stable";
        } else if (energyLevel >= 45) {
            energyStatus = "strained";
        } else {
            energyStatus = "critical";
        }

        double generation = roundOneDecimal(energyLevel * 1.35 + playerCount * 6.0 + maxStage * 9.0);
        double consumptionTarget = generation * (0.68 + 0.04 * Math.max(1, playerCount));
        double consumption = roundOneDecimal(Math.min(generation - 2.0, consumptionTarget));
        double reserve = roundOneDecimal(Math.max(0.0, generation - consumption));
        statusWriter.updateEnergy(energyStatus, energyLevel, generation, consumption, reserve, now);

        int pendingCount = countPendingIntents();
        List<RiskEntry> risks = new ArrayList<>();
        if (pendingCount > 0) {
            String severity = pendingCount >= 5 ? "high" : "medium";
            risks.add(RiskEntry.of(
                    "intent_backlog",
                    "Manifestation intent backlog",
                    severity,
                    "There are " + pendingCount + " pending intents awaiting processing.",
                    now));
        }
        if (energyLevel < 45) {
            String severity = energyLevel < 30 ? "high" : "medium";
            risks.add(RiskEntry.of(
                    "energy_grid_strain",
                    "Energy grid strain",
                    severity,
                    "Operational energy level has fallen to " + energyLevel + "%.",
                    now));
        }
        statusWriter.replaceRisks(risks, now);
    }

    private int countPendingIntents() {
        if (pendingDir == null) {
            return 0;
        }
        if (!Files.isDirectory(pendingDir)) {
            return 0;
        }
        try (var stream = Files.list(pendingDir)) {
            int count = 0;
            var iterator = stream.iterator();
            while (iterator.hasNext()) {
                Path candidate = iterator.next();
                if (Files.isRegularFile(candidate)) {
                    count++;
                }
            }
            return count;
        } catch (IOException ex) {
            CrystalTech.LOGGER.warn("Failed to enumerate pending intents at {}", pendingDir, ex);
            return 0;
        }
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

    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
