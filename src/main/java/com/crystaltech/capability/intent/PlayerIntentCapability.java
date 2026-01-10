package com.crystaltech.capability.intent;

import com.crystaltech.CrystalTech;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Capability definition for player manifestation intents.
 */
public final class PlayerIntentCapability {
    public static final ResourceLocation KEY = new ResourceLocation(CrystalTech.MOD_ID, "player_intent");
    public static final Capability<IPlayerIntent> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private static boolean logged;

    private PlayerIntentCapability() {
    }

    public static void logRegistered() {
        if (!logged) {
            CrystalTech.LOGGER.info("Player intent capability registered.");
            logged = true;
        }
    }
}
