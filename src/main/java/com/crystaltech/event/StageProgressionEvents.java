package com.crystaltech.event;

import java.time.Instant;

import com.crystaltech.CrystalTech;
import com.crystaltech.capability.intent.PlayerIntentCapability;
import com.crystaltech.core.CrystalStageApi;
import com.crystaltech.protocol.ManifestationIntentLogger;
import com.crystaltech.protocol.ManifestationIntentService;
import com.crystaltech.registry.ModItems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Governs stage transitions triggered by player crafting and future machine outputs.
 */
@Mod.EventBusSubscriber(modid = CrystalTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StageProgressionEvents {
    private StageProgressionEvents() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        StageTransition transition = StageTransition.fromCrafting(CrystalStageApi.getStage(player), event.getCrafting());
        if (transition == null) {
            return;
        }

        Instant now = Instant.now();
        var lazyIntent = player.getCapability(PlayerIntentCapability.CAPABILITY);
        var intent = lazyIntent.orElse(null);
        if (intent == null) {
            ManifestationIntentLogger.logStageBlocked(player, transition.nextStage(), "intent_capability_missing");
            return;
        }

        if (!intent.hasActiveIntent()) {
            ManifestationIntentLogger.logStageBlocked(player, transition.nextStage(), "manifestation_intent_missing");
            return;
        }
        if (intent.isExpired(now)) {
            ManifestationIntentLogger.logStageBlocked(player, transition.nextStage(), "manifestation_intent_expired");
            return;
        }
        if (!intent.isStageAllowed(transition.nextStage(), now)) {
            ManifestationIntentLogger.logStageBlocked(player, transition.nextStage(), "allowed_stage_insufficient");
            return;
        }

        CrystalTech.LOGGER.debug("Attempting stage transition {} -> {} via crafting for {}", transition.currentStage(), transition.nextStage(), player.getGameProfile().getName());
        boolean advanced = CrystalStageApi.tryAdvance(player, transition.nextStage());
        if (!advanced) {
            CrystalTech.LOGGER.debug("Stage transition rejected for {}", player.getGameProfile().getName());
            return;
        }

        ManifestationIntentService.getInstance().markStageConsumed(player, transition.nextStage(), now);
        ManifestationIntentLogger.logApplied(player, transition.nextStage());
        CrystalTech.LOGGER.debug("Stage transition complete for {}", player.getGameProfile().getName());
    }

    private record StageTransition(int currentStage, int nextStage) {
        private static StageTransition fromCrafting(int currentStage, ItemStack craftedStack) {
            if (currentStage == StageIds.BASELINE && (craftedStack.is(ModItems.AMETHYST_POWDER.get()) || craftedStack.is(ModItems.QUARTZ_POWDER.get()))) {
                return new StageTransition(currentStage, StageIds.MATERIALIZATION);
            }
            return null;
        }
    }

    private static final class StageIds {
        private static final int BASELINE = 0;
        private static final int MATERIALIZATION = 1;

        private StageIds() {
        }
    }
}
