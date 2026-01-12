package com.crystaltech.core.tag;

import com.crystaltech.CrystalTech;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Centralised tag definitions used across CrystalTech systems.
 */
public final class ModTags {
    private ModTags() {
    }

    public static final class Blocks {
        public static final TagKey<Block> NEEDS_AMETHYST_ALLOY_TOOL = create("needs_amethyst_alloy_tool");

        private Blocks() {
        }

        private static TagKey<Block> create(String id) {
            return TagKey.create(Registries.BLOCK, new ResourceLocation(CrystalTech.MOD_ID, id));
        }
    }

    public static final class Items {
        public static final TagKey<Item> AMETHYST_MATERIALS = create("amethyst_materials");

        private Items() {
        }

        private static TagKey<Item> create(String id) {
            return TagKey.create(Registries.ITEM, new ResourceLocation(CrystalTech.MOD_ID, id));
        }
    }
}
