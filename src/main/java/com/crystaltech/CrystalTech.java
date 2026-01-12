package com.crystaltech;

import org.slf4j.Logger;

import com.crystaltech.client.CrystalTechClientLifecycle;
import com.crystaltech.event.ModLifecycleEvents;
import com.crystaltech.registry.ModBlockEntities;
import com.crystaltech.registry.ModBlocks;
import com.crystaltech.registry.ModItems;
import com.crystaltech.registry.ModMenus;
import com.mojang.logging.LogUtils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
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
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModLifecycleEvents.register(modEventBus);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CrystalTechClientLifecycle.register(modEventBus));
    }
}
