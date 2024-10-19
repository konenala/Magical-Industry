package com.github.nalamodikk.block.entity.mana_crafting_table;

import com.github.nalamodikk.block.entity.mana_crafting_table.ManaCraftingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdvancedManaCraftingTableBlockEntity extends ManaCraftingTableBlockEntity {
    private final ItemStackHandler inputHandler = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler outputHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final LazyOptional<ItemStackHandler> inputHandlerOptional = LazyOptional.of(() -> inputHandler);
    private final LazyOptional<ItemStackHandler> outputHandlerOptional = LazyOptional.of(() -> outputHandler);

    public AdvancedManaCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (side == null) {
                return inputHandlerOptional.cast();
            } else if (side == net.minecraft.core.Direction.DOWN) {
                return outputHandlerOptional.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void dropContents() {
        for (int i = 0; i < inputHandler.getSlots(); i++) {
            ItemStack stack = inputHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack));
            }
        }
        for (int i = 0; i < outputHandler.getSlots(); i++) {
            ItemStack stack = outputHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack));
            }
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputHandlerOptional.invalidate();
        outputHandlerOptional.invalidate();
    }
}