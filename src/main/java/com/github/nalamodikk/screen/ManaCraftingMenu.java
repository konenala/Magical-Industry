package com.github.nalamodikk.screen;

import com.github.nalamodikk.block.entity.ManaCraftingTableBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class ManaCraftingMenu extends AbstractContainerMenu {
    private final ManaCraftingTableBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final IItemHandler itemHandler;
    private final DataSlot manaStored = DataSlot.standalone();

    public ManaCraftingTableBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public ManaCraftingMenu(int containerId, Inventory playerInventory, IItemHandler itemHandler, ContainerLevelAccess access, Level level) {
        super(ModMenusTypes.MANA_CRAFTING_MENU.get(), containerId);
        this.access = access;
        this.itemHandler = itemHandler;

        // 初始化 blockEntity 變量
        this.blockEntity = (ManaCraftingTableBlockEntity) access.evaluate((world, pos) -> {
            if (world != null) {
                return world.getBlockEntity(pos);
            }
            return null;
        }).orElse(null);

        // 設置 3x3 合成槽
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new SlotItemHandler(itemHandler, j + i * 3, 30 + j * 18, 17 + i * 18));
            }
        }

        // 設置輸出槽
        this.addSlot(new SlotItemHandler(itemHandler, 9, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 輸出槽不允許手動放入物品
            }
        });

        // 設置玩家的物品欄槽
        addPlayerInventorySlots(playerInventory);

        // 同步魔力值
        this.addDataSlot(manaStored);
    }


    // 获取当前的魔力存储量
    public int getManaStored() {
        return manaStored.get();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            if (index < 10) { // 如果是合成槽或输出槽
                if (!this.moveItemStackTo(stackInSlot, 10, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // 如果是玩家物品栏
                if (!this.moveItemStackTo(stackInSlot, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity != null ? blockEntity.getBlockState().getBlock() : null);
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        // 主物品栏
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 快捷栏
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
            int currentMana = blockEntity.getManaStored();
            manaStored.set(currentMana);  // 更新魔力值
        }
    }

}