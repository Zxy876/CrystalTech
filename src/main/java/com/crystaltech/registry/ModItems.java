package com.crystaltech.registry;

import com.crystaltech.CrystalTech;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Holds item registrations for CrystalTech.
 */
public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CrystalTech.MOD_ID);

    public static final RegistryObject<Item> AMETHYST_ALLOY = ITEMS.register("amethyst_alloy",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_RECONSTRUCTOR = ITEMS.register("crystal_reconstructor",
            () -> new Item(new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
