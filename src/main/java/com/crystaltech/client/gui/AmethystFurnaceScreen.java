package com.crystaltech.client.gui;

import com.crystaltech.content.menu.AmethystFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client GUI for the Amethyst furnace. Reuses vanilla furnace texture while layering custom energy info.
 */
public class AmethystFurnaceScreen extends AbstractContainerScreen<AmethystFurnaceMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    private static final int ENERGY_BAR_X = 10;
    private static final int ENERGY_BAR_Y = 17;
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 52;

    public AmethystFurnaceScreen(AmethystFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHoveringEnergyBar(mouseX, mouseY)) {
            int stored = menu.getData().get(2);
            int capacity = menu.getData().get(3);
            guiGraphics.renderTooltip(font, Component.translatable("tooltip.crystaltech.energy", stored, capacity), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        int progress = menu.getProgressScaled(24);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, x + 79, y + 34, 176, 14, progress + 1, 16);
        }

        int energy = menu.getEnergyScaled(ENERGY_BAR_HEIGHT);
        if (energy > 0) {
            int top = y + ENERGY_BAR_Y + (ENERGY_BAR_HEIGHT - energy);
            guiGraphics.fill(x + ENERGY_BAR_X, top, x + ENERGY_BAR_X + ENERGY_BAR_WIDTH, y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT, 0xFFAA5CFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
        if (menu.isMultiblockFormed()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.crystaltech.amethyst_furnace.multiblock"), 98, 6, 0x4CAF50, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("screen.crystaltech.amethyst_furnace.multiblock_missing"), 88, 6, 0xAA0000, false);
        }
    }

    private boolean isHoveringEnergyBar(int mouseX, int mouseY) {
        int relativeX = mouseX - (leftPos + ENERGY_BAR_X);
        int relativeY = mouseY - (topPos + ENERGY_BAR_Y);
        return relativeX >= 0 && relativeX < ENERGY_BAR_WIDTH && relativeY >= 0 && relativeY < ENERGY_BAR_HEIGHT;
    }
}
