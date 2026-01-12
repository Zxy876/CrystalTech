package com.crystaltech.registry;

import com.crystaltech.CrystalTech;
import com.crystaltech.content.menu.AmethystFurnaceMenu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Menu registrations needed across client and server.
 */
public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CrystalTech.MOD_ID);

    public static final RegistryObject<MenuType<AmethystFurnaceMenu>> AMETHYST_FURNACE = MENUS.register(
            "amethyst_furnace",
            () -> IForgeMenuType.create(AmethystFurnaceMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
