package com.github.nalamodikk.screen.mana_crafting_table;

import com.github.nalamodikk.block.entity.mana_crafting_table.AdvancedManaCraftingTableBlockEntity;
import com.github.nalamodikk.recipe.ManaCraftingTableRecipe;
import com.github.nalamodikk.screen.ModMenusTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AdvancedManaCraftingTableMenu extends BaseManaCraftingMenu {
    private static final Logger LOGGER = LogManager.getLogger();
    private final AdvancedManaCraftingTableBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final DataSlot manaStored = DataSlot.standalone();

    public static final int OUTPUT_SLOT = 9;
    public static final int INPUT_SLOT_START = 0;
    public static final int INPUT_SLOT_END = 8;

    public AdvancedManaCraftingTableMenu(int containerId, Inventory playerInventory, AdvancedManaCraftingTableBlockEntity blockEntity) {
        super(ModMenusTypes.ADVANCED_MANA_CRAFTING_TABLE_MENU.get(), containerId, playerInventory, blockEntity.getItemHandler(), ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), blockEntity.getLevel(), blockEntity);


        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

    IItemHandler inputHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElseThrow(IllegalStateException::new);

        // 添加输入槽位
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new SlotItemHandler(inputHandler, j + i * 3, 30 + j * 18, 17 + i * 18));
            }
        }

        // 添加输出槽位
        this.addSlot(new SlotItemHandler(inputHandler, OUTPUT_SLOT, 124, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (blockEntity != null) {
                    Optional<ManaCraftingTableRecipe> recipe = blockEntity.getCurrentRecipe();

                    if (recipe.isPresent() && blockEntity.hasSufficientMana(recipe.get().getManaCost())) {
                        blockEntity.consumeMana(recipe.get().getManaCost());

                        for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                            blockEntity.getItemHandler().extractItem(i, 1, false);
                        }

                        blockEntity.updateCraftingResult();
                    }
                }
                super.onTake(player, stack);
            }
        });

        // 玩家物品栏
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 玩家快捷栏
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

        // 同步魔力值
        this.addDataSlot(manaStored);
    }


    public int getManaStored() {
        return this.manaStored.get();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
            manaStored.set(blockEntity.getManaStored());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack originalStack = stackInSlot.copy();

        if (index == OUTPUT_SLOT) {
            if (blockEntity == null || !blockEntity.hasRecipe()) {
                return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(stackInSlot, 10, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }

            slot.onQuickCraft(stackInSlot, originalStack);
        } else if (index >= 10) {
            if (!this.moveItemStackTo(stackInSlot, INPUT_SLOT_START, INPUT_SLOT_END + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= INPUT_SLOT_START && index <= INPUT_SLOT_END) {
            if (!this.moveItemStackTo(stackInSlot, 10, this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return originalStack;
    }
}
