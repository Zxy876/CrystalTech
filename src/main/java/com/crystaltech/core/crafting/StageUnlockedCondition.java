package com.crystaltech.core.crafting;

import com.crystaltech.CrystalTech;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * Placeholder data condition gate used to declare the minimum unlocked stage for a recipe.
 * Currently always returns true but encodes the requirement for future runtime enforcement.
 */
public record StageUnlockedCondition(int stage) implements ICondition {
    public static final ResourceLocation ID = new ResourceLocation(CrystalTech.MOD_ID, "stage_unlocked");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return true;
    }

    public static final class Serializer implements IConditionSerializer<StageUnlockedCondition> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public void write(JsonObject json, StageUnlockedCondition value) {
            json.addProperty("stage", value.stage());
        }

        @Override
        public StageUnlockedCondition read(JsonObject json) {
            int stage = json.has("stage") ? json.get("stage").getAsInt() : 0;
            return new StageUnlockedCondition(stage);
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
