package com.crystaltech.registry.tier;

import com.crystaltech.core.tag.ModTags;
import com.crystaltech.registry.ModItems;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

/**
 * Tool tiers added by CrystalTech.
 */
public final class ModTiers {
    public static final Tier AMETHYST_ALLOY = new ForgeTier(
            3,
            300,
            7.0F,
            2.5F,
            18,
            ModTags.Blocks.NEEDS_AMETHYST_ALLOY_TOOL,
            () -> Ingredient.of(ModItems.AMETHYST_ALLOY_INGOT.get()));

    private ModTiers() {
    }
}
