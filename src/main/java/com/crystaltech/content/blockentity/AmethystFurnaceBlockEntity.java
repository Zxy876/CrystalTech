package com.crystaltech.content.blockentity;

import java.util.UUID;

import javax.annotation.Nullable;

import com.crystaltech.content.block.AmethystFurnaceBlock;
import com.crystaltech.content.menu.AmethystFurnaceMenu;
import com.crystaltech.event.StageProgressionEvents;
import com.crystaltech.registry.ModBlockEntities;
import com.crystaltech.registry.ModBlocks;
import com.crystaltech.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Manages processing logic, energy storage and inventory for the Amethyst furnace.
 */
public class AmethystFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_AUXILIARY = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_ENERGY = 3;
    public static final int SLOT_COUNT = 4;

    public static final int PROCESS_TIME_TICKS = 200;
    public static final int ENERGY_COST_PER_TICK = 40;
    public static final int MAX_ENERGY = 40_000;
    private static final int MAX_ENERGY_TRANSFER = 400;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_INPUT -> stack.is(ModItems.AMETHYST_POWDER.get());
                case SLOT_AUXILIARY -> stack.is(ModItems.QUARTZ_POWDER.get());
                case SLOT_OUTPUT -> false;
                default -> true;
            };
        }
    };
    private final LazyOptional<ItemStackHandler> itemCapability = LazyOptional.of(() -> items);

    private final AmethystEnergyStorage energyStorage = new AmethystEnergyStorage(MAX_ENERGY, MAX_ENERGY_TRANSFER);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);

    private final ContainerData dataAccess = new SimpleContainerData(5);

    private int progress;
    private boolean multiblockFormed;
    private UUID activeOperator;

    public AmethystFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMETHYST_FURNACE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmethystFurnaceBlockEntity blockEntity) {
        blockEntity.ensureDataSync();
        boolean wasWorking = blockEntity.isProcessing();

        blockEntity.pullEnergyFromContainer();

        if (blockEntity.canProcess()) {
            if (blockEntity.tryConsumeEnergy(ENERGY_COST_PER_TICK)) {
                blockEntity.progress++;
                if (blockEntity.progress >= PROCESS_TIME_TICKS) {
                    blockEntity.completeProcessing();
                    blockEntity.progress = 0;
                }
            } else {
                blockEntity.cooldownProgress();
            }
        } else {
            blockEntity.cooldownProgress();
        }

        boolean isWorking = blockEntity.isProcessing();
        if (wasWorking != isWorking) {
            blockEntity.updateLitState(level, pos, state, isWorking);
        }

        blockEntity.ensureDataSync();
        blockEntity.setChanged();
    }

    private void cooldownProgress() {
        if (progress > 0) {
            progress = Math.max(0, progress - 2);
        }
    }

    private boolean canProcess() {
        if (level == null || level.isClientSide) {
            return false;
        }
        ItemStack input = items.getStackInSlot(SLOT_INPUT);
        ItemStack aux = items.getStackInSlot(SLOT_AUXILIARY);
        if (input.isEmpty() || aux.isEmpty()) {
            return false;
        }
        if (!input.is(ModItems.AMETHYST_POWDER.get()) || !aux.is(ModItems.QUARTZ_POWDER.get())) {
            return false;
        }
        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        if (!output.isEmpty() && (!output.is(ModItems.AMETHYST_ALLOY_INGOT.get()) || output.getCount() >= output.getMaxStackSize())) {
            return false;
        }
        if (energyStorage.getEnergyStored() < ENERGY_COST_PER_TICK) {
            return false;
        }
        return true;
    }

    private boolean tryConsumeEnergy(int amount) {
        int extracted = energyStorage.extractEnergy(amount, true);
        if (extracted < amount) {
            return false;
        }
        energyStorage.extractEnergy(amount, false);
        return true;
    }

    private void completeProcessing() {
        ItemStack result = new ItemStack(ModItems.AMETHYST_ALLOY_INGOT.get());

        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            items.setStackInSlot(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }

        items.extractItem(SLOT_INPUT, 1, false);
        items.extractItem(SLOT_AUXILIARY, 1, false);
        setChanged();

        if (!level.isClientSide) {
            ServerPlayer player = resolveActiveOperator();
            if (player != null) {
                StageProgressionEvents.handleMachineOutput(player, result.copy());
            }
        }
    }

    private void pullEnergyFromContainer() {
        ItemStack stack = items.getStackInSlot(SLOT_ENERGY);
        if (stack.isEmpty()) {
            return;
        }
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(provider -> {
            int space = energyStorage.getSpace();
            if (space <= 0) {
                return;
            }
            int transferred = provider.extractEnergy(Math.min(MAX_ENERGY_TRANSFER, space), false);
            if (transferred > 0) {
                energyStorage.receiveEnergy(transferred, false);
                setChanged();
            }
        });
    }

    private void ensureDataSync() {
        dataAccess.set(0, progress);
        dataAccess.set(1, PROCESS_TIME_TICKS);
        dataAccess.set(2, energyStorage.getEnergyStored());
        dataAccess.set(3, energyStorage.getMaxEnergyStored());
        dataAccess.set(4, multiblockFormed ? 1 : 0);
    }

    private void updateLitState(Level level, BlockPos pos, BlockState state, boolean isWorking) {
        if (state.getValue(AmethystFurnaceBlock.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(AmethystFurnaceBlock.LIT, isWorking), Block.UPDATE_ALL);
            setChanged();
        }
    }

    public void updateMultiblockState() {
        if (level == null) {
            return;
        }
        boolean formed = true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos checkPos = worldPosition.offset(dx, 0, dz);
                if (!level.getBlockState(checkPos).is(ModBlocks.AMETHYST_BRICKS.get())) {
                    formed = false;
                    break;
                }
            }
            if (!formed) {
                break;
            }
        }
        if (formed != multiblockFormed) {
            multiblockFormed = formed;
            setChanged();
        }
        ensureDataSync();
    }

    private boolean isProcessing() {
        return progress > 0 && canProcess();
    }

    public void setActiveOperator(Player player) {
        if (player != null) {
            activeOperator = player.getUUID();
        }
    }

    @Nullable
    private ServerPlayer resolveActiveOperator() {
        if (activeOperator == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(activeOperator);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /**
     * Adds energy directly to the internal buffer (used by tests or debug tooling).
     */
    public void addEnergy(int amount) {
        energyStorage.receiveEnergy(amount, false);
        ensureDataSync();
        setChanged();
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public boolean isMultiblockFormed() {
        return multiblockFormed;
    }

    public int getComparatorOutput() {
        SimpleContainer container = new SimpleContainer(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            container.setItem(i, items.getStackInSlot(i));
        }
        return AbstractContainerMenu.getRedstoneSignalFromContainer(container);
    }

    public void dropContents(Level level, BlockPos pos) {
        SimpleContainer container = new SimpleContainer(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            container.setItem(i, items.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, container);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.put("Energy", energyStorage.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putBoolean("MultiblockFormed", multiblockFormed);
        if (activeOperator != null) {
            tag.putUUID("ActiveOperator", activeOperator);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        energyStorage.deserializeNBT(tag.getCompound("Energy"));
        progress = tag.getInt("Progress");
        multiblockFormed = tag.getBoolean("MultiblockFormed");
        if (tag.hasUUID("ActiveOperator")) {
            activeOperator = tag.getUUID("ActiveOperator");
        } else {
            activeOperator = null;
        }
        ensureDataSync();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        load(packet.getTag());
    }

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        if (capability == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        energyCapability.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.crystaltech.amethyst_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        setActiveOperator(player);
        return new AmethystFurnaceMenu(id, inventory, this, dataAccess);
    }

    private class AmethystEnergyStorage extends EnergyStorage {
        private AmethystEnergyStorage(int capacity, int maxTransfer) {
            super(capacity, maxTransfer, maxTransfer);
        }

        private int getSpace() {
            return getMaxEnergyStored() - getEnergyStored();
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }

    }
}
