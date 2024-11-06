package com.github.nalamodikk.common.screen.tool;


import com.github.nalamodikk.common.screen.ModMenusTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;


public class UniversalConfigMenu extends AbstractContainerMenu {

    private final BlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public UniversalConfigMenu(int id, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenusTypes.UNIVERSAL_CONFIG.get(), id);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        layoutPlayerInventorySlots(playerInventory, 8, 84);

    }

    private void layoutPlayerInventorySlots(Inventory playerInventory, int leftCol, int topRow) {
        // 玩家物品欄槽位 (3行)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, leftCol + col * 18, topRow + row * 18));
            }
        }

        // 玩家快捷欄槽位 (1行)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, leftCol + col * 18, topRow + 58));
        }
    }

    public UniversalConfigMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, Objects.requireNonNull(playerInventory.player.level().getBlockEntity(buf.readBlockPos())));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

}