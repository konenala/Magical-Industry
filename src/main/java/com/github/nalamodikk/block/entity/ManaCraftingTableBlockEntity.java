package com.github.nalamodikk.block.entity;

import com.github.nalamodikk.Capability.IMana;
import com.github.nalamodikk.Capability.ManaStorage;
import com.github.nalamodikk.Capability.ModCapabilities;
import com.github.nalamodikk.recipe.ManaCraftingTableRecipe;
import com.github.nalamodikk.screen.ManaCraftingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ManaCraftingTableBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(10); // 9 个合成材料 + 1 个输出槽
    private static final int INPUT_SLOT_START = 0;
    private static final int INPUT_SLOT_END = 8;
    private static final int OUTPUT_SLOT = 9;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA);
    private final LazyOptional<IMana> manaOptional = LazyOptional.of(() -> manaStorage);

    public static final int MAX_MANA = 1000;
    private static final int MANA_COST_PER_CRAFT = 50;

    public ManaCraftingTableBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(), pPos, pBlockState);
    }

    public void craftItem() {
        Optional<ManaCraftingTableRecipe> recipe = getCurrentRecipe();

        if (recipe.isPresent() && hasEnoughMana(MANA_COST_PER_CRAFT)) {
            ItemStack result = recipe.get().assemble(new SimpleContainer(itemHandler.getSlots()), level.registryAccess());

            // 消耗魔力
            consumeMana(MANA_COST_PER_CRAFT);

            // 清空 3x3 合成表中的每个槽位
            for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                this.itemHandler.extractItem(i, 1, false);
            }

            // 将合成结果放到输出槽
            this.itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(result.getItem(),
                    this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + result.getCount()));
        }
    }

    private Optional<ManaCraftingTableRecipe> getCurrentRecipe() {
        SimpleContainer inventory = new SimpleContainer(9);
        for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
            inventory.setItem(i - INPUT_SLOT_START, this.itemHandler.getStackInSlot(i));
        }

        return this.level.getRecipeManager().getRecipeFor(ManaCraftingTableRecipe.Type.INSTANCE, inventory, level);
    }

    public boolean hasRecipe() {
        Optional<ManaCraftingTableRecipe> recipe = getCurrentRecipe();

        if (recipe.isEmpty()) {
            return false;
        }
        ItemStack result = recipe.get().assemble(new SimpleContainer(itemHandler.getSlots()), level.registryAccess());

        return canInsertAmountIntoOutputSlot(result.getCount()) && canInsertItemIntoOutputSlot(result.getItem());
    }

    public boolean hasEnoughMana(int requiredMana) {
        return manaStorage.getMana() >= requiredMana;
    }

    public int getManaStored() {
        return manaStorage.getMana();
    }

    // 添加魔力
    public void addMana(int amount) {
        // 添加魔力
        this.getCapability(ModCapabilities.MANA).ifPresent(mana -> {
            mana.addMana(amount);
            System.out.println("Mana added: " + amount + ", Current Mana: " + mana.getMana());

            setChanged(); // 通知伺服器端數據已更改

            // 更新客戶端
            if (!this.level.isClientSide()) {
                BlockState state = this.level.getBlockState(worldPosition);
                this.level.sendBlockUpdated(this.worldPosition, state, state, 3); // 通知客戶端方塊狀態已更新
            }

            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        });

        // 調用方法以更新當前的合成結果
        updateCraftingResult();
    }



    // 消耗魔力
    public void consumeMana(int amount) {
        manaStorage.consumeMana(amount);
        System.out.println("Mana consumed: " + amount + ", Current Mana: " + manaStorage.getMana());
        setChanged(); // 通知服务器数据已经更改
        if (!level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3); // 触发客户端更新

        }
        if (!level.isClientSide()) {
            setChanged();
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3); // 確保數據變更通知客戶端
        }

    }



    private void updateCraftingResult() {
        Optional<ManaCraftingTableRecipe> recipe = getCurrentRecipe();
        if (recipe.isPresent()) {
            ItemStack result = recipe.get().assemble(new SimpleContainer(itemHandler.getSlots()), level.registryAccess());
            this.itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            this.itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private boolean canInsertItemIntoOutputSlot(Item item) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() || this.itemHandler.getStackInSlot(OUTPUT_SLOT).is(item);
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + count <= this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        } else if (cap == ModCapabilities.MANA) {
            return manaOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        manaOptional.invalidate();
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magical_industry.mana_crafting_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new ManaCraftingMenu(pContainerId, pPlayerInventory, itemHandler, ContainerLevelAccess.create(this.level, this.worldPosition), this.level);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("ManaStored", manaStorage.getMana());
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        manaStorage.setMana(pTag.getInt("ManaStored"));
    }

    public static class Provider implements ICapabilityProvider {
        private final ManaCraftingTableBlockEntity blockEntity;
        private final LazyOptional<IMana> manaOptional;

        public Provider(ManaCraftingTableBlockEntity blockEntity) {
            this.blockEntity = blockEntity;
            this.manaOptional = LazyOptional.of(() -> blockEntity.manaStorage);
        }

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ModCapabilities.MANA) {
                return manaOptional.cast();
            }
            return LazyOptional.empty();
        }
    }
}
