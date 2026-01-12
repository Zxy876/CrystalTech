package com.crystaltech.gametest;

import com.crystaltech.CrystalTech;
import com.crystaltech.content.blockentity.AmethystFurnaceBlockEntity;
import com.crystaltech.registry.ModBlocks;
import com.crystaltech.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraft.world.item.ItemStack;

/**
 * Basic gametests covering Amethyst furnace processing behaviour.
 */
@GameTestHolder(CrystalTech.MOD_ID)
public final class AmethystFurnaceGameTests {
    private AmethystFurnaceGameTests() {
    }

    @GameTest(template = "minecraft:empty")
    public static void furnaceProducesIngot(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        helper.setBlock(BlockPos.ZERO, ModBlocks.AMETHYST_FURNACE.get());

        AmethystFurnaceBlockEntity furnace = getFurnace(helper, origin);
        furnace.addEnergy(AmethystFurnaceBlockEntity.MAX_ENERGY);
        furnace.getItems().setStackInSlot(AmethystFurnaceBlockEntity.SLOT_INPUT, new ItemStack(ModItems.AMETHYST_POWDER.get()));
        furnace.getItems().setStackInSlot(AmethystFurnaceBlockEntity.SLOT_AUXILIARY, new ItemStack(ModItems.QUARTZ_POWDER.get()));

        helper.runAtTickTime(AmethystFurnaceBlockEntity.PROCESS_TIME_TICKS + 5, () -> {
            ItemStack output = furnace.getItems().getStackInSlot(AmethystFurnaceBlockEntity.SLOT_OUTPUT);
            if (!output.is(ModItems.AMETHYST_ALLOY_INGOT.get())) {
                helper.fail("Amethyst furnace did not produce alloy ingot", origin);
                return;
            }
            helper.succeed();
        });
    }

    private static AmethystFurnaceBlockEntity getFurnace(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof AmethystFurnaceBlockEntity furnace) {
            return furnace;
        }
        throw new IllegalStateException("Expected amethyst furnace block entity at " + pos);
    }
}
