package com.github.nalamodikk.common.block.blockentity.mana_crafting;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.network.mana_net.ManaNetworkManager;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.recipe.ManaCraftingTableRecipe;
import com.github.nalamodikk.common.screen.manacrafting.ManaCraftingMenu;
import com.github.nalamodikk.common.config.ManaConduitConfigLoader;
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
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ManaCraftingTableBlockEntity extends BlockEntity implements MenuProvider, IUnifiedManaHandler {
    private final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (slot < 9) {
                updateCraftingResult();
            }
        }
    }; // 9 个合成材料 + 1 个输出槽
    private static final int INPUT_SLOT_START = 0;
    private static final int INPUT_SLOT_END = 8;
    private static final int OUTPUT_SLOT = 9;
    private int manaCheckCooldown = 10; // 每 10 tick 檢查一次
    // 用於存儲上次的魔力值，默認為 0
    private int lastManaValue = 0;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA);
    private LazyOptional<IUnifiedManaHandler> lazyManaStorage = LazyOptional.of(() -> manaStorage);


    public static final int MAX_MANA = 10000;
    private static final int MANA_COST_PER_CRAFT = 50;

    public ManaCraftingTableBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(), pPos, pBlockState);
        lazyManaStorage = LazyOptional.of(() -> manaStorage); // 確保在構造時初始化
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaCraftingTableBlockEntity blockEntity) {
        if (!level.isClientSide) {
            if (blockEntity.manaCheckCooldown-- <= 0) {
                // 1️⃣ 從鄰近的魔力設備提取魔力
                blockEntity.extractManaFromNeighbors();

                // 2️⃣ 從導管網路請求魔力（改為讀取 `ManaConduitConfigLoader` 的傳輸速率）
                ManaNetworkManager manager = ManaNetworkManager.getInstance(level);
                int maxTransferRate = ManaConduitConfigLoader.getTransferRate(); // 讀取目前設定的導管傳輸速率
                int manaRequested = Math.min(maxTransferRate, blockEntity.getNeededMana(0)); // 限制請求量不超過設定值
                int manaReceived = (int) manager.requestMana(pos, manaRequested);

                if (manaReceived > 0) {
                    blockEntity.addMana(manaReceived, ManaAction.EXECUTE);
                    MagicalIndustryMod.LOGGER.debug("ManaCraftingTable requested {} mana and received {}", manaRequested, manaReceived);
                }

                // 重設冷卻時間
                blockEntity.manaCheckCooldown = 10;
            }
        }
    }



    public void extractManaFromNeighbors() {
        if (level == null || level.isClientSide) return;

        // 如果魔力槽已滿，不執行提取邏輯
        if (manaStorage.isFull()) return;

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor != null) {
                neighbor.getCapability(ModCapabilities.MANA, direction.getOpposite()).ifPresent(neighborManaStorage -> {
                    int manaNeeded = manaStorage.getNeeded();
                    if (manaNeeded <= 0) return;

                    // 嘗試從鄰近方塊提取魔力
                    int extracted = neighborManaStorage.extractMana(manaNeeded, ManaAction.EXECUTE);
                    if (extracted > 0) {
                        manaStorage.addMana(extracted, ManaAction.EXECUTE);

                        setChanged(); // 標記數據已更改

                        // 如果已經填滿，就提前結束迴圈
                        if (manaStorage.isFull()) return;
                    }
                });
            }
        }
    }



    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
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

    public Optional<ManaCraftingTableRecipe> getCurrentRecipe() {
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

    public boolean hasSufficientMana(int requiredMana) {
        return manaStorage.getMana() >= requiredMana;
    }

    // 添加魔力
    public void addMana(int amount) {
        // 添加魔力
        this.getCapability(ModCapabilities.MANA).ifPresent(mana -> {
            // 使用 addMana 方法增加魔力
            mana.addMana(amount, ManaAction.get(true));
            System.out.println("Mana added: " + amount + ", Current Mana: " + mana.getMana());

            // 通知伺服器端數據已更改
            setChanged();

            // 更新客戶端
            if (!this.level.isClientSide()) {
                BlockState state = this.level.getBlockState(worldPosition);
                this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
            }
        });

        // 調用方法以更新當前的合成結果
        updateCraftingResult();
    }


    @Override
    public int getMana() {
        return manaStorage.getMana();
    }

    @Override
    public void addMana(int amount, ManaAction action) {
        int inserted = manaStorage.insertMana(0, amount, action); // ✅ 使用 `insertMana()` 確保一致性
        if (inserted > 0) {
            setChanged();
            updateClient();
            MagicalIndustryMod.LOGGER.debug("ManaCraftingTable received {} mana at {}", inserted, worldPosition);
        }
    }

    @Override
    public void consumeMana(int amount) {
        int extracted = manaStorage.extractMana(0, amount, ManaAction.EXECUTE); // ✅ 使用 `extractMana()` 確保一致性
        if (extracted > 0) {
            setChanged();
            updateClient();
            MagicalIndustryMod.LOGGER.debug("ManaCraftingTable consumed {} mana at {}", extracted, worldPosition);
        }
    }


    @Override
    public void setMana(int amount) {
        manaStorage.setMana(Math.min(Math.max(amount, 0), MAX_MANA)); // 確保魔力值在範圍內
        setChanged();
        updateClient();
    }

    @Override
    public void onChanged() {
        setChanged();
        updateClient();
    }

    @Override
    public int getMaxMana() {
        return MAX_MANA; // 設定最大魔力值
    }

    @Override
    public int getManaContainerCount() {
        return 1; // 只有一個魔力儲存槽
    }

    @Override
    public int getMana(int container) {
        if (container != 0) throw new IndexOutOfBoundsException("Invalid container index: " + container);
        return manaStorage.getMana();
    }

    @Override
    public void setMana(int container, int mana) {
        if (container != 0) throw new IndexOutOfBoundsException("Invalid container index: " + container);
        setMana(mana); // 調用 `setMana(int amount)`
    }

    @Override
    public int getMaxMana(int container) {
        if (container != 0) throw new IndexOutOfBoundsException("Invalid container index: " + container);
        return MAX_MANA;
    }

    @Override
    public int getNeededMana(int container) {
        if (container != 0) throw new IndexOutOfBoundsException("Invalid container index: " + container);
        return MAX_MANA - manaStorage.getMana(); // 返回還能儲存的魔力量
    }



    private void updateClient() {
        if (level != null && !level.isClientSide()) {
            setChanged();
            if (lastManaValue != manaStorage.getMana()) {
                lastManaValue = manaStorage.getMana();
                BlockState state = level.getBlockState(worldPosition);
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    }



    public void updateCraftingResult() {
        Optional<ManaCraftingTableRecipe> recipe = getCurrentRecipe();
        if (recipe.isPresent() && hasSufficientMana(recipe.get().getManaCost())) {
            ItemStack result = recipe.get().assemble(new SimpleContainer(itemHandler.getSlots()), level.registryAccess());
            this.itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            this.itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        }

        // 確保狀態已更新，通知客戶端
        setChanged(); // 標記區塊狀態已更改
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public void onSlotChanged() {
        // 檢查並更新合成結果
        updateCraftingResult();
    }

    private boolean canInsertItemIntoOutputSlot(Item item) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() || this.itemHandler.getStackInSlot(OUTPUT_SLOT).is(item);
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + count <= this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ManaCapability.MANA) {
            if (lazyManaStorage == null) { // ✅ 先檢查 `lazyManaStorage`
                lazyManaStorage = LazyOptional.of(() -> manaStorage);
            }
            return lazyManaStorage.cast();
        }
        return super.getCapability(cap, side);
    }


    @Override
    public void onLoad() {
        super.onLoad();
        if (!lazyItemHandler.isPresent()) {
            lazyItemHandler = LazyOptional.of(() -> itemHandler);
        }
        if (!lazyManaStorage.isPresent()) { // ✅ 只檢查 `isPresent()`，避免 `NullPointerException`
            lazyManaStorage = LazyOptional.of(() -> manaStorage);
        }
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
        super.saveAdditional(pTag);
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("ManaStored", manaStorage.getMana());
    }



    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        manaStorage.setMana(pTag.getInt("ManaStored"));
    }

    @Override
    public int insertMana(int container, int amount, ManaAction action) {
        return manaStorage.insertMana(container, amount, action); // ✅ 直接呼叫 `manaStorage.insertMana()`

    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        int extracted = manaStorage.extractMana(container, amount, action);
        if (extracted > 0) {
            setChanged();
            updateClient();
        }
        return extracted;
    }

}
