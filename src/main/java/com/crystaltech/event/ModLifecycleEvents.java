package com.crystaltech.event;

import com.crystaltech.capability.CrystalStageCapability;
import com.crystaltech.capability.ICrystalStage;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Registers listeners bound to the mod event bus. Forge invokes these during
 * the mod loading lifecycle.
 */
public final class ModLifecycleEvents {
    private ModLifecycleEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModLifecycleEvents::onRegisterCapabilities);
    }

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ICrystalStage.class);
        CrystalStageCapability.logRegistered();
    }
}
