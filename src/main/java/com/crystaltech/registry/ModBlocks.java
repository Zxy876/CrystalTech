package com.crystaltech.registry;

import java.util.function.Supplier;

import com.crystaltech.CrystalTech;
import com.crystaltech.content.block.AmethystFluxCableBlock;
import com.crystaltech.content.block.AmethystFurnaceBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers blocks and their corresponding block items.
 */
public final class ModBlocks {
    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CrystalTech.MOD_ID);

    public static final RegistryObject<Block> AMETHYST_FURNACE = register("amethyst_furnace",
            () -> new AmethystFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.BLAST_FURNACE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> AMETHYST_BRICKS = register("amethyst_bricks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.PURPUR_BLOCK)
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()));

        public static final RegistryObject<Block> AMETHYST_FLUX_CABLE = register("amethyst_flux_cable",
            () -> new AmethystFluxCableBlock(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                .strength(0.4F)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 7)
                .noOcclusion()
                .requiresCorrectToolForDrops()));

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> blockSupplier) {
        RegistryObject<T> block = BLOCKS.register(name, blockSupplier);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
