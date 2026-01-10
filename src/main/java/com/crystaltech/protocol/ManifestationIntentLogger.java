package com.crystaltech.protocol;

import java.time.Instant;

import com.crystaltech.CrystalTech;

import net.minecraft.world.entity.player.Player;

/**
 * Centralised logging helper for all manifestation intent processing.
 */
public final class ManifestationIntentLogger {
    private ManifestationIntentLogger() {
    }

    public static void logReceived(ManifestationIntent intent, java.util.UUID playerId) {
        CrystalTech.LOGGER.info("Received manifestation intent {} for player {} (allowed_stage={})", intent.intentId(), playerId, intent.allowedStage());
    }

    public static void logRejected(ManifestationIntent intent, java.util.UUID playerId, String reason) {
        CrystalTech.LOGGER.warn("Rejected manifestation intent {} for player {}: {}", intent.intentId(), playerId, reason);
    }

    public static void logIgnored(ManifestationIntent intent, java.util.UUID playerId, String reason) {
        CrystalTech.LOGGER.info("Ignored manifestation intent {} for player {}: {}", intent.intentId(), playerId, reason);
    }

    public static void logExpired(ManifestationIntent intent, java.util.UUID playerId, Instant now) {
        CrystalTech.LOGGER.warn("Manifestation intent {} for player {} expired at {} (now={})", intent.intentId(), playerId, intent.expiresAt(), now);
    }

    public static void logApplied(Player player, int stage) {
        CrystalTech.LOGGER.info("Applied manifestation allowance for {} -> stage {}", player.getGameProfile().getName(), stage);
    }

    public static void logStageBlocked(Player player, int targetStage, String reason) {
        CrystalTech.LOGGER.debug("Stage {} progression for {} blocked: {}", targetStage, player.getGameProfile().getName(), reason);
    }
}
