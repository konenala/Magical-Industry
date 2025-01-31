package com.github.nalamodikk.common.block.blockentity.mana_crafting;

import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.network.mana_net.NetworkManager;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.recipe.ManaCraftingTableRecipe;
import com.github.nalamodikk.common.screen.manacrafting.ManaCraftingMenu;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ManaCraftingTableBlockEntity extends BlockEntity implements MenuProvider {

    private static final int INPUT_SLOT_START = 0;
    private static final int INPUT_SLOT_END = 8;
    private static final int OUTPUT_SLOT = 9;
    public static final int MAX_MANA = 10000;
    private static final int MANA_COST_PER_CRAFT = 50;
    private static final int MANA_CHECK_INTERVAL = 10;

    private final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot < OUTPUT_SLOT) updateCraftingResult();
            setChanged();
        }
    };

    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);
    private LazyOptional<ManaStorage> lazyManaStorage = LazyOptional.of(() -> manaStorage);
    private int manaCheckCooldown = MANA_CHECK_INTERVAL;

    public ManaCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(), pos, state);
    }

    // 每 tick 伺服端邏輯
    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaCraftingTableBlockEntity blockEntity) {
        if (!level.isClientSide && blockEntity.manaCheckCooldown-- <= 0) {
            blockEntity.extractManaFromNeighbors();
            blockEntity.manaCheckCooldown = MANA_CHECK_INTERVAL;
        }
    }

    private void extractManaFromNeighbors() {
        if (level == null || manaStorage.isFull()) return;

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) continue;

            neighbor.getCapability(ModCapabilities.MANA, direction.getOpposite()).ifPresent(neighborManaStorage -> {
                int manaNeeded = manaStorage.getNeeded();
                if (manaNeeded > 0) {
                    int extracted = neighborManaStorage.extractMana(manaNeeded, ManaAction.get(true));
                    manaStorage.addMana(extracted);
                    setChanged();
                }
            });

            if (manaStorage.isFull()) break;
        }
    }

    public boolean craftItem() {
        Optional<ManaCraftingTableRecipe> recipe = getCurrentRecipe();
        if (recipe.isEmpty() || !hasEnoughMana(MANA_COST_PER_CRAFT)) return false;

        // 消耗魔力並清空輸入槽
        consumeMana(MANA_COST_PER_CRAFT);
        for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
            itemHandler.extractItem(i, 1, false);
        }

        // 更新輸出槽
        ItemStack result = recipe.get().assemble(new SimpleContainer(itemHandler.getSlots()), level.registryAccess());
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (outputSlot.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else if (outputSlot.sameItem(result) && ItemStack.tagMatches(outputSlot, result)) {
            // 確保相同物品並且 NBT 屬性一致
            outputSlot.grow(result.getCount());
        } else {
            // 如果物品不同，直接返回 false（避免覆蓋輸出槽）
            return false;
        }

        setChanged();
        return true;
    }


    private Optional<ManaCraftingTableRecipe> getCurrentRecipe() {
        SimpleContainer inventory = new SimpleContainer(9);
        for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
            inventory.setItem(i - INPUT_SLOT_START, itemHandler.getStackInSlot(i));
        }
        return level.getRecipeManager().getRecipeFor(ManaCraftingTableRecipe.Type.INSTANCE, inventory, level);
    }

    private void updateCraftingResult() {
        Optional<ManaCraftingTableRecipe> recipe = getCurrentRecipe();
        if (recipe.isPresent() && hasEnoughMana(recipe.get().getManaCost())) {
            ItemStack result = recipe.get().assemble(new SimpleContainer(itemHandler.getSlots()), level.registryAccess());
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        }
        setChanged();
    }

    private boolean hasEnoughMana(int requiredMana) {
        return manaStorage.getMana() >= requiredMana;
    }

    private boolean consumeMana(int amount) {
        if (manaStorage.getMana() < amount) return false;

        manaStorage.consumeMana(amount);
        updateClient();
        return true;
    }

    private void updateClient() {
        if (level != null && !level.isClientSide) {
            setChanged();
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inventory);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        if (cap == ManaCapability.MANA) return lazyManaStorage.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyManaStorage = LazyOptional.of(() -> manaStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyManaStorage.invalidate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("ManaStored", manaStorage.getMana());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        manaStorage.setMana(tag.getInt("ManaStored"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magical_industry.mana_crafting_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ManaCraftingMenu(id, playerInventory, itemHandler, ContainerLevelAccess.create(level, worldPosition), level);
    }
}
