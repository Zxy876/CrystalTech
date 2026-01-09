package com.crystaltech.core;

import java.util.Optional;

import com.crystaltech.capability.CrystalStageCapability;
import com.crystaltech.capability.ICrystalStage;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

/**
 * Lightweight helper for querying and mutating the player crystal stage.
 */
public final class CrystalStageApi {
    private CrystalStageApi() {
    }

    public static Optional<ICrystalStage> get(Player player) {
        return player.getCapability(CrystalStageCapability.CAPABILITY).resolve();
    }

    public static int getStage(Player player) {
        return get(player).map(ICrystalStage::getStage).orElse(ICrystalStage.MIN_STAGE);
    }

    public static boolean isAtLeast(Player player, int stage) {
        return getStage(player) >= stage;
    }

    public static boolean tryAdvance(Player player, int targetStage) {
        return get(player).map(capability -> {
            int clamped = Math.min(ICrystalStage.MAX_STAGE, Math.max(ICrystalStage.MIN_STAGE, targetStage));
            int oldStage = capability.getStage();
            if (clamped <= oldStage) {
                return false;
            }
            capability.setStage(clamped);
            MinecraftForge.EVENT_BUS.post(new CrystalStageChangedEvent(player, oldStage, clamped));
            return true;
        }).orElse(false);
    }

    public static void setStage(Player player, int stage) {
        get(player).ifPresent(capability -> capability.setStage(stage));
    }
}
