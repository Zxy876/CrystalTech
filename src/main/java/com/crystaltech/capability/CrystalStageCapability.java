package com.crystaltech.capability;

import com.crystaltech.CrystalTech;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Central access point for the crystal stage capability metadata.
 */
public final class CrystalStageCapability {
    public static final ResourceLocation KEY = new ResourceLocation(CrystalTech.MOD_ID, "crystal_stage");
    public static final Capability<ICrystalStage> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private static boolean logged;

    private CrystalStageCapability() {
    }

    public static void logRegistered() {
        if (!logged) {
            CrystalTech.LOGGER.info("Crystal stage capability registered.");
            logged = true;
        }
    }
}
