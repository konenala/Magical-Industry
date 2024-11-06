package com.github.nalamodikk.common.screen.ManaGenerator;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.ManaGenerator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.screen.ModMenusTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.common.ForgeHooks;
import net.minecraft.tags.ItemTags;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;


public class ManaGeneratorMenu extends AbstractContainerMenu {
    private final ManaGeneratorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public ManaGeneratorMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        super(ModMenusTypes.MANA_GENERATOR_MENU.get(), id);
        BlockPos pos = buf.readBlockPos();
        Level level = playerInventory.player.level();
        this.blockEntity = (ManaGeneratorBlockEntity) level.getBlockEntity(pos);
        this.access = ContainerLevelAccess.create(level, pos);

        if (blockEntity != null) {
            this.data = blockEntity.getContainerData();
            addDataSlots(this.data);
            IItemHandler blockInventory = blockEntity.getInventory();
            // 添加機器的燃料槽
            this.addSlot(new SlotItemHandler(blockInventory, 0, 80, 40) {
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
        } else {
            throw new IllegalStateException("BlockEntity at pos " + pos + " is not present or not correct type.");
        }

        // 添加玩家物品欄和快捷欄槽位
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

    public int getMaxMana() {
        return ManaGeneratorBlockEntity.MAX_MANA;
    }

    public int getMaxEnergy() {
        return ManaGeneratorBlockEntity.MAX_ENERGY;
    }

    public BlockPos getBlockEntityPos() {
        return this.blockEntity.getBlockPos();
    }

    public void toggleCurrentMode() {
        int currentMode = this.getCurrentMode();
        this.data.set(ManaGeneratorBlockEntity.MODE_INDEX, currentMode == 0 ? 1 : 0); // 2 對應於模式索引，0: MANA, 1: ENERGY
    }

    public void saveModeState() {
        if (blockEntity != null) {
            blockEntity.markUpdated(); // 確保保存當前模式到世界中
        }
    }

    public int getCurrentMode() {
        return this.data.get(ManaGeneratorBlockEntity.MODE_INDEX);
    }

    public int getManaStored() {
        return this.data.get(ManaGeneratorBlockEntity.MANA_STORED_INDEX);
    }

    public int getEnergyStored() {
        int energyStored = this.data.get(ManaGeneratorBlockEntity.ENERGY_STORED_INDEX);
//        MagicalIndustryMod.LOGGER.info("Client Energy (Menu): " + energyStored);
        return energyStored;
    }

    public int getBurnTime() {
        return this.data.get(ManaGeneratorBlockEntity.BURN_TIME_INDEX);
    }

    public int getCurrentBurnTime() {
        return this.data.get(ManaGeneratorBlockEntity.CURRENT_BURN_TIME_INDEX);
    }
}
