package com.crystaltech.registry;

import com.crystaltech.CrystalTech;
import com.crystaltech.content.blockentity.AmethystFluxCableBlockEntity;
import com.crystaltech.content.blockentity.AmethystFurnaceBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Holds block entity registrations for CrystalTech.
 */
public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CrystalTech.MOD_ID);

    public static final RegistryObject<BlockEntityType<AmethystFurnaceBlockEntity>> AMETHYST_FURNACE = BLOCK_ENTITIES.register(
            "amethyst_furnace",
            () -> BlockEntityType.Builder.of(AmethystFurnaceBlockEntity::new, ModBlocks.AMETHYST_FURNACE.get()).build(null));

        public static final RegistryObject<BlockEntityType<AmethystFluxCableBlockEntity>> AMETHYST_FLUX_CABLE = BLOCK_ENTITIES.register(
            "amethyst_flux_cable",
            () -> BlockEntityType.Builder.of(AmethystFluxCableBlockEntity::new, ModBlocks.AMETHYST_FLUX_CABLE.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
