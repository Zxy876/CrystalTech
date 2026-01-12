package com.crystaltech.registry;

import com.crystaltech.CrystalTech;
import com.crystaltech.registry.tier.ModTiers;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Holds item registrations for CrystalTech.
 */
public final class ModItems {
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CrystalTech.MOD_ID);

    public static final RegistryObject<Item> AMETHYST_POWDER = ITEMS.register("amethyst_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> QUARTZ_POWDER = ITEMS.register("quartz_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> AMETHYST_ALLOY_INGOT = ITEMS.register("amethyst_alloy_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> AMETHYST_ALLOY_PICKAXE = ITEMS.register("amethyst_alloy_pickaxe",
            () -> new PickaxeItem(ModTiers.AMETHYST_ALLOY, 1, -2.8F, new Item.Properties()));

    public static final RegistryObject<Item> AMETHYST_ALLOY_SWORD = ITEMS.register("amethyst_alloy_sword",
            () -> new SwordItem(ModTiers.AMETHYST_ALLOY, 3, -2.4F, new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
