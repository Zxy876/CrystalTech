package com.crystaltech.content.menu;

import java.util.Objects;

import com.crystaltech.content.blockentity.AmethystFurnaceBlockEntity;
import com.crystaltech.registry.ModBlocks;
import com.crystaltech.registry.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Menu/controller for the Amethyst furnace. Handles inventory slots and client data sync.
 */
public class AmethystFurnaceMenu extends AbstractContainerMenu {
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_COLUMNS = 9;
    private static final int PLAYER_INV_SIZE = PLAYER_INV_ROWS * PLAYER_INV_COLUMNS;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INV_SIZE;
    private static final int BLOCK_SLOT_COUNT = AmethystFurnaceBlockEntity.SLOT_COUNT;
    private static final int TOTAL_SLOT_COUNT = VANILLA_SLOT_COUNT + BLOCK_SLOT_COUNT;

    private final AmethystFurnaceBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public AmethystFurnaceMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(windowId, playerInventory, getBlockEntity(playerInventory, buffer), new SimpleContainerData(5));
    }

    public AmethystFurnaceMenu(int windowId, Inventory playerInventory, AmethystFurnaceBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.AMETHYST_FURNACE.get(), windowId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = data;

        addBlockSlots(blockEntity.getItems());
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    private static AmethystFurnaceBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buffer) {
        Objects.requireNonNull(playerInventory, "playerInventory");
        Objects.requireNonNull(buffer, "buffer");
        BlockPos pos = buffer.readBlockPos();
        Level level = playerInventory.player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AmethystFurnaceBlockEntity furnace) {
            return furnace;
        }
        throw new IllegalStateException("Unable to find Amethyst furnace at " + pos);
    }

    private void addBlockSlots(IItemHandler handler) {
        this.addSlot(new SlotItemHandler(handler, AmethystFurnaceBlockEntity.SLOT_INPUT, 56, 17));
        this.addSlot(new SlotItemHandler(handler, AmethystFurnaceBlockEntity.SLOT_AUXILIARY, 33, 35));
        this.addSlot(new SlotItemHandler(handler, AmethystFurnaceBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                blockEntity.setActiveOperator(player);
            }
        });
        this.addSlot(new SlotItemHandler(handler, AmethystFurnaceBlockEntity.SLOT_ENERGY, 8, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
            }
        });
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < PLAYER_INV_ROWS; ++row) {
            for (int column = 0; column < PLAYER_INV_COLUMNS; ++column) {
                int slotIndex = column + row * 9 + 9;
                int x = 8 + column * 18;
                int y = 84 + row * 18;
                this.addSlot(new Slot(inventory, slotIndex, x, y));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < HOTBAR_SLOT_COUNT; ++slot) {
            this.addSlot(new Slot(inventory, slot, 8 + slot * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.AMETHYST_FURNACE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack movedStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            movedStack = stack.copy();
            if (index < VANILLA_SLOT_COUNT) {
                if (!moveItemStackTo(stack, VANILLA_SLOT_COUNT, TOTAL_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return movedStack;
    }

    public int getProgressScaled(int width) {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }
        return progress * width / maxProgress;
    }

    public int getEnergyScaled(int height) {
        int energy = data.get(2);
        int capacity = data.get(3);
        if (capacity <= 0) {
            return 0;
        }
        return Math.min(height, (int) Math.round((double) energy * height / capacity));
    }

    public boolean isMultiblockFormed() {
        return data.get(4) == 1;
    }

    public boolean hasOutput() {
        IItemHandler handler = blockEntity.getItems();
        ItemStack output = handler.getStackInSlot(AmethystFurnaceBlockEntity.SLOT_OUTPUT);
        return !output.isEmpty();
    }

    public ContainerData getData() {
        return data;
    }
}
