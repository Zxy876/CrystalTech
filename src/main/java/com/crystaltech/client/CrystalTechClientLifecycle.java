package com.crystaltech.client;

import com.crystaltech.client.gui.AmethystFurnaceScreen;
import com.crystaltech.registry.ModMenus;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registers client-only listeners for CrystalTech.
 */
public final class CrystalTechClientLifecycle {
    private CrystalTechClientLifecycle() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CrystalTechClientLifecycle::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.AMETHYST_FURNACE.get(), AmethystFurnaceScreen::new));
    }
}
