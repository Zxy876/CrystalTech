package com.crystaltech.event;

import com.crystaltech.CrystalTech;
import com.crystaltech.core.CrystalStageApi;
import com.crystaltech.registry.ModItems;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Governs stage transitions triggered by contextual item use.
 */
@Mod.EventBusSubscriber(modid = CrystalTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StageProgressionEvents {
    private StageProgressionEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        StageTransition transition = StageTransition.resolve(CrystalStageApi.getStage(player), event.getItemStack());
        if (transition == null) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        CrystalTech.LOGGER.debug("Attempting stage transition {} -> {} for {}", transition.currentStage(), transition.nextStage(), player.getGameProfile().getName());
        boolean advanced = CrystalStageApi.tryAdvance(player, transition.nextStage());
        if (!advanced) {
            CrystalTech.LOGGER.debug("Stage transition rejected for {}", player.getGameProfile().getName());
            return;
        }

        if (!transition.reward().isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, transition.reward().copy());
        }

        CrystalTech.LOGGER.debug("Stage transition complete for {}", player.getGameProfile().getName());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private record StageTransition(int currentStage, int nextStage, ItemStack reward) {
        private static StageTransition resolve(int currentStage, ItemStack stack) {
            if (currentStage == 0 && stack.is(Items.AMETHYST_SHARD)) {
                return new StageTransition(currentStage, 1, new ItemStack(ModItems.AMETHYST_ALLOY.get()));
            }
            if (currentStage == 1 && stack.is(ModItems.AMETHYST_ALLOY.get())) {
                return new StageTransition(currentStage, 2, new ItemStack(ModItems.CRYSTAL_RECONSTRUCTOR.get()));
            }
            return null;
        }
    }
}
