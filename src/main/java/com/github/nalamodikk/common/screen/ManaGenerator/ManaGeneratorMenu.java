package com.github.nalamodikk.common.screen.ManaGenerator;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.screen.ModMenusTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.common.ForgeHooks;
import net.minecraft.tags.ItemTags;
import org.jetbrains.annotations.NotNull;

public class ManaGeneratorMenu extends AbstractContainerMenu {
    private final ManaGeneratorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public ManaGeneratorMenu(int id, Inventory playerInventory, BlockPos pos, Level level, ItemStackHandler itemStackHandler) {
        super(ModMenusTypes.MANA_GENERATOR_MENU.get(), id);
        this.blockEntity = (ManaGeneratorBlockEntity) level.getBlockEntity(pos);
        this.access = ContainerLevelAccess.create(level, pos);
        this.data = blockEntity.getContainerData();
        this.addDataSlots(this.data);
        if (blockEntity != null) {
            IItemHandler blockInventory = blockEntity.getInventory();
            this.addSlot(new SlotItemHandler(blockInventory, 0, 80, 45) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    // 根據當前模式設置不同的物品允許邏輯
                    if (blockEntity.getCurrentMode() == 0) { // MANA mode
                        return stack.is(ItemTags.create(new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana")));
                    } else { // ENERGY mode
                        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
                    }
                }
            });
        }

        // 添加玩家物品欄和快捷欄槽位
        layoutPlayerInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            if (index < 1) { // 如果是在方塊槽位
                if (!this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) { // 如果是在玩家槽位
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public int getCurrentMode() {
        return this.data.get(2); // 2 對應於模式
    }

    public int getManaStored() {
        return this.data.get(0); // 0 對應於魔力儲存
    }

    public int getEnergyStored() {
        return this.data.get(1); // 1 對應於能量儲存
    }

    public int getMaxMana() {
        return ManaGeneratorBlockEntity.MAX_MANA;
    }

    public int getMaxEnergy() {
        return 10000;
    }

    public BlockPos getBlockEntityPos() {
        return this.blockEntity.getBlockPos();
    }

    public void toggleCurrentMode() {
        int currentMode = this.getCurrentMode();
        this.data.set(2, currentMode == 0 ? 1 : 0); // 2 對應於模式索引，0: MANA, 1: ENERGY
    }

    public void saveModeState() {
        if (blockEntity != null) {
            blockEntity.markUpdated(); // 確保保存當前模式到世界中
        }
    }

    public ManaGeneratorBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public int getBurnTime() {
        return this.data.get(4); // 4 對應於當前燃燒時間
    }

    public int getCurrentBurnTime() {
        return this.data.get(5); // 5 對應於總燃燒時間
    }

}
