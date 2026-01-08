package com.crystaltech;

import org.slf4j.Logger;

import com.crystaltech.event.ModLifecycleEvents;
import com.crystaltech.registry.ModItems;
import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Entry point for the CrystalTech mod. Responsible for registering core systems
 * and bridging the mod event bus with Forge's global event bus.
 */
@Mod(CrystalTech.MOD_ID)
public class CrystalTech {
    public static final String MOD_ID = "crystaltech";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrystalTech() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModLifecycleEvents.register(modEventBus);
    }
}
