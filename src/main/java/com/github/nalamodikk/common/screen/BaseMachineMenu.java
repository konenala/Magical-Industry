package com.github.nalamodikk.common.screen;


import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class BaseMachineMenu extends AbstractContainerMenu {
    protected final BlockEntity blockEntity;
    protected final ContainerData data;

    protected BaseMachineMenu(MenuType<?> type, int id, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(type, id);
        this.blockEntity = blockEntity;
        this.data = data;
        addDataSlots(data);
    }

    public int getEnergyStored() {
        return data.get(0);
    }

    public int getProgress() {
        return data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 需要根據機械的槽位邏輯來改寫
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved();
    }
}
