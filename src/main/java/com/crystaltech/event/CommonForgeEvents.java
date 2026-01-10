package com.crystaltech.event;

import com.crystaltech.CrystalTech;
import com.crystaltech.capability.CrystalStageCapability;
import com.crystaltech.capability.CrystalStageProvider;
import com.crystaltech.capability.intent.PlayerIntentCapability;
import com.crystaltech.capability.intent.PlayerIntentProvider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles Forge-wide events such as capability attachment and player cloning.
 */
@Mod.EventBusSubscriber(modid = CrystalTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonForgeEvents {
    private CommonForgeEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            CrystalStageProvider stageProvider = new CrystalStageProvider();
            event.addCapability(CrystalStageCapability.KEY, stageProvider);
            event.addListener(stageProvider::invalidate);

            PlayerIntentProvider intentProvider = new PlayerIntentProvider();
            event.addCapability(PlayerIntentCapability.KEY, intentProvider);
            event.addListener(intentProvider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        original.getCapability(CrystalStageCapability.CAPABILITY).ifPresent(oldCap ->
                clone.getCapability(CrystalStageCapability.CAPABILITY).ifPresent(newCap ->
                        newCap.setStage(oldCap.getStage())));
        original.getCapability(PlayerIntentCapability.CAPABILITY).ifPresent(oldCap ->
                clone.getCapability(PlayerIntentCapability.CAPABILITY).ifPresent(newCap ->
                        newCap.copyFrom(oldCap)));
    }
}
