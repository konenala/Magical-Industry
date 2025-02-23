package com.github.nalamodikk.common.block.blockentity.ManaGenerator;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.NeoMagnaMod;
import com.github.nalamodikk.common.block.block.ManaGenerator.ManaGeneratorBlock;
import com.github.nalamodikk.common.network.mana_net.ManaNetworkManager;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.compat.energy.UnifiedEnergyStorage;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.register.ConfigManager;
import com.github.nalamodikk.common.screen.ManaGenerator.ManaGeneratorMenu;
import com.github.nalamodikk.common.sync.UnifiedSyncManager;
import com.github.nalamodikk.common.util.loader.FuelRateLoader;
import com.github.nalamodikk.common.util.loader.ManaGenerator.BurnTimeFuelLoader;
import com.github.nalamodikk.common.util.loader.ManaGenerator.ManaFuelLoader;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicLong;

public class ManaGeneratorBlockEntity extends BlockEntity implements GeoBlockEntity, GeoAnimatable, MenuProvider , IConfigurableBlock {

    public static final int MANA_STORED_INDEX = 0;
    public static final int ENERGY_STORED_INDEX = 1;
    public static final int MODE_INDEX = 2;
    public static final int BURN_TIME_INDEX = 3;
    public static final int CURRENT_BURN_TIME_INDEX = 4;
    private final UnifiedSyncManager syncManager = new UnifiedSyncManager(5); // 假設有 5 個需要同步的數據
    private int lastEnergyStored = -1;
    private int lastManaStored = -1;
    private Mode lastMode = null;
    private int lastBurnTime = -1;
    private int lastCurrentBurnTime = -1;


    private static int getConfigEnergyRate() {
        return ConfigManager.COMMON.energyRate.get();
    }

    private static int getConfigManaRate() {
        return ConfigManager.COMMON.manaRate.get();
    }


    public static final int MAX_MANA = 30000; // 或者保留 private，然後新增 getter
    public static final int MAX_ENERGY = 32000;

    private final UnifiedEnergyStorage energyStorage = new UnifiedEnergyStorage(MAX_ENERGY);
    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA);
    private final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (currentMode == Mode.ENERGY) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                FuelRateLoader.FuelRate fuelRate = FuelRateLoader.getFuelRateForItem(itemId);
                return (fuelRate != null && fuelRate.getBurnTime() > 0) || (ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0);
            } else if (currentMode == Mode.MANA) {
                return stack.is(ItemTags.create(new ResourceLocation(NeoMagnaMod.MOD_ID, "mana"))); // 魔力模式只允許標籤為 mana 的物品
            }
            return false;
        }
    };

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private boolean isWorking = false;
    private Mode currentMode = Mode.MANA; // 默認模式
    private int burnTime;
    private int currentBurnTime;
    private double energyAccumulated = 0.0;
    private double manaAccumulated = 0.0;
//    int storedEnergy = energyStorage.getEnergyStored();

    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("working");
    private final LazyOptional<ItemStackHandler> lazyFuelHandler = LazyOptional.of(() -> fuelHandler);
    private final LazyOptional<UnifiedEnergyStorage> lazyEnergyStorage = LazyOptional.of(() -> energyStorage);
    private final LazyOptional<ManaStorage> lazyManaStorage = LazyOptional.of(() -> manaStorage);
    private final EnumMap<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);

    public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state);
    }

    public void markUpdated() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(fuelHandler.getSlots());
        for (int i = 0; i < fuelHandler.getSlots(); i++) {
            inventory.setItem(i, fuelHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public void toggleMode() {
        currentMode = (currentMode == Mode.MANA) ? Mode.ENERGY : Mode.MANA;
        markUpdated();
    }

    public int getStoredEnergy() {
        return energyStorage.getEnergyStored();
    }

    public ContainerData getContainerData() {
        return syncManager.getContainerData();
    }

    public ItemStackHandler getInventory() {
        return fuelHandler;
    }

    public int getCurrentMode() {
        return (currentMode == Mode.MANA) ? 0 : 1;
    }


    public static void tick(Level level, BlockPos pos, BlockState state, ManaGeneratorBlockEntity blockEntity) {
        serverTick(level, pos, state, blockEntity);
    }

    @Override
    public void setDirectionConfig(Direction direction, boolean isOutput) {
        directionConfig.put(direction, isOutput);
        markUpdated(); // 標記更新
    }


    // 確認某個方向是否為輸出
    @Override
    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }



    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaGeneratorBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.sync(); // 同步到客戶端

            // **1️⃣ 生成魔力**
            long generatedMana = blockEntity.generateEnergyOrMana();

            // **2️⃣ 透過網路傳輸魔力**
            ManaNetworkManager manager = ManaNetworkManager.getInstance(level); // 取得對應世界的魔力網路管理器
            long excessMana = manager.insertMana(pos, generatedMana); // 插入到魔力網路

            // **3️⃣ 剩餘的魔力存回自己的儲存器**
            blockEntity.manaStorage.insertMana((int) excessMana, ManaAction.EXECUTE);

            // **4️⃣ 嘗試輸出魔力到周圍機器**
            blockEntity.outputEnergyAndMana();

            blockEntity.markUpdated(); // 標記數據更新
        }
    }


    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ManaNetworkManager manager = ManaNetworkManager.getInstance(level);
            manager.registerMachine(worldPosition, 1); // 註冊為可接收魔力的機器
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            if (!isStateChanged()) {

                return; // 如果狀態沒有更改，則跳過同步
            }

            // 更新同步數據
            syncManager.set(ENERGY_STORED_INDEX, energyStorage.getEnergyStored());
            syncManager.set(MANA_STORED_INDEX, manaStorage.getMana());
            syncManager.set(MODE_INDEX, currentMode == Mode.MANA ? 0 : 1);
            syncManager.set(BURN_TIME_INDEX, burnTime);
            syncManager.set(CURRENT_BURN_TIME_INDEX, currentBurnTime);

            // 發送方塊更新事件
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();

            NeoMagnaMod.LOGGER.debug("State changed: Energy={}, Mana={}, Mode={}, BurnTime={}, CurrentBurnTime={}",
                    lastEnergyStored != energyStorage.getEnergyStored(),
                    lastManaStored != manaStorage.getMana(),
                    lastMode != currentMode,
                    lastBurnTime != burnTime,
                    lastCurrentBurnTime != currentBurnTime);

            // 更新上次的狀態快照
            updateLastState();
        }
    }

    private boolean isStateChanged() {
        // 比較當前狀態與上次的快照
        return lastEnergyStored != energyStorage.getEnergyStored() ||
                lastManaStored != manaStorage.getMana() ||
                lastMode != currentMode ||
                lastBurnTime != burnTime ||
                lastCurrentBurnTime != currentBurnTime;
    }

    private void updateLastState() {
        // 保存當前狀態為快照
        lastEnergyStored = energyStorage.getEnergyStored();
        lastManaStored = manaStorage.getMana();
        lastMode = currentMode;
        lastBurnTime = burnTime;
        lastCurrentBurnTime = currentBurnTime;
    }


    private long insertIntoNetwork(Level level, BlockPos pos, long mana) {
        if (mana > 0) {
            ManaNetworkManager manager = ManaNetworkManager.getInstance(level); // 獲取 NetworkManager 實例
            return manager.insertMana(pos, mana); // 使用實例方法
        }
        return mana; // 如果沒有成功插入，返回原始 mana
    }

    private long generateEnergyOrMana() {
        long generatedAmount = 0;

        // 如果存儲已滿，不要燃燒燃料
        if (isStorageFull()) {
            return 0;
        }

        // 如果燃燒時間結束，嘗試初始化新的燃燒
        if (burnTime <= 0) {
            if (fuelHandler.getStackInSlot(0).isEmpty()) {
                setWorking(false);
                return 0;
            }
            initializeBurn(fuelHandler.getStackInSlot(0));
        }

        if (burnTime > 0) {
            burnTime--;

            if (burnTime == 0) {
                setWorking(false);
            }

            // 根據模式發電或產生魔力
            if (currentMode == Mode.ENERGY) {
                generatedAmount = generateEnergy();
            } else if (currentMode == Mode.MANA) {
                generatedAmount = generateMana();
            }
        }

        return generatedAmount;
    }

    private void initializeBurn(ItemStack fuel) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(fuel.getItem());

        if (currentMode == Mode.ENERGY) {
            burnTime = BurnTimeFuelLoader.getBurnTime(itemId);
        } else if (currentMode == Mode.MANA) {
            burnTime = ManaFuelLoader.getManaFuelValue(itemId);
        }

        if (burnTime > 0) {
            fuelHandler.extractItem(0, 1, false); // 消耗燃料
            setWorking(true); // 設置為工作狀態
        } else {
            setWorking(false); // 如果燃燒時間為 0，停止工作
        }
    }

    private boolean isStorageFull() {
        return (currentMode == Mode.ENERGY && energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) ||
                (currentMode == Mode.MANA && manaStorage.getMana() >= manaStorage.getMaxMana());
    }



    // 檢查是否需要停止工作
    private boolean shouldStopWorking() {
        ItemStack fuel = fuelHandler.getStackInSlot(0);

        // 如果燃料槽為空且燃燒時間耗盡，停止工作
        if (fuel.isEmpty() && burnTime <= 0) {
            isWorking = false;
            return true;
        }

        // 如果存儲已滿，停止工作
        if (isStorageFull()) {
            setWorking(false);
            return true;
        }

        return false;
    }

    // 初始化燃燒（如果需要）
    private boolean initializeBurnIfNecessary() {
        ItemStack fuel = fuelHandler.getStackInSlot(0);

        if (!fuel.isEmpty()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(fuel.getItem());
            int burnTimeForFuel = BurnTimeFuelLoader.getBurnTime(itemId);
            int manaValue = ManaFuelLoader.getManaFuelValue(itemId);

            // 確保燃燒時間有效
            if (burnTimeForFuel > 0 || manaValue > 0) {
                burnTime = (currentMode == Mode.ENERGY) ? burnTimeForFuel : manaValue;
                fuelHandler.extractItem(0, 1, false); // 消耗燃料
                setWorking(true); // 設置為工作狀態
                return true;
            }
        }
        return false;
    }

    // 處理燃燒過程
    private void processBurn() {
        if (burnTime > 0) {
            burnTime--;

            // 當燃燒結束時停止工作
            if (burnTime == 0) {
                setWorking(false);
            }

            // 根據模式執行對應的邏輯
            if (currentMode == Mode.ENERGY) {
                generateEnergy();
            } else if (currentMode == Mode.MANA) {
                generateMana();
            }
        }
    }



    // ==========================================================================
    private long generateEnergy() {
        if (energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) {
            isWorking = false;
            return 0; // 如果能量已滿，返回 0
        }

        energyAccumulated += getConfigEnergyRate();
        long generatedEnergy = 0;

        if (energyAccumulated >= 1.0) {
            generatedEnergy = (long) energyAccumulated;
            energyAccumulated -= generatedEnergy;
            energyStorage.receiveEnergy((int) generatedEnergy, false);
        }

        return generatedEnergy;
    }


// ==========================================================================

    private long generateMana() {
        if (manaStorage.getMana() >= manaStorage.getMaxMana()) {
            isWorking = false;
            return 0; // 如果魔力已滿，返回 0
        }

        manaAccumulated += getConfigManaRate();
        long generatedMana = 0;

        if (manaAccumulated >= 1.0) {
            generatedMana = (long) manaAccumulated;
            manaAccumulated -= generatedMana;
            manaStorage.addMana((int) generatedMana, ManaAction.EXECUTE);        }

        return generatedMana;
    }

// ==========================================================================


    public void outputEnergyAndMana() {
        if (level == null) {
            return; // 確保世界不為空
        }

        ManaNetworkManager manager = ManaNetworkManager.getInstance(level); // 取得魔力網路管理器

        // **嘗試透過導管網路輸出魔力**
        AtomicLong excessMana = new AtomicLong(manager.insertMana(worldPosition, 50));
//        MagicalIndustryMod.LOGGER.debug("Mana Generator at {} attempting to insert mana. Excess: {}", worldPosition, excessMana.get());

        // 遍歷所有方向
        for (Direction direction : Direction.values()) {
            // 檢查該方向是否為輸出方向
            if (!isOutput(direction)) {
                continue; // 如果不是輸出方向，直接跳過
            }

            BlockEntity neighborBlockEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (neighborBlockEntity == null) {
                continue; // 如果該方向沒有方塊，跳過
            }

            // **嘗試將能量輸出到相鄰方塊**
            neighborBlockEntity.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(neighborEnergyStorage -> {
                if (neighborEnergyStorage.canReceive()) {
                    int energyToTransfer = Math.min(energyStorage.getEnergyStored(), 100); // 限制每次最多傳輸 100 能量
                    int acceptedEnergy = neighborEnergyStorage.receiveEnergy(energyToTransfer, false);
                    energyStorage.extractEnergy(acceptedEnergy, false); // 從自己的能量儲存中提取
                    NeoMagnaMod.LOGGER.debug("Transferred {} energy to {} at direction {}", acceptedEnergy, neighborBlockEntity.getBlockPos(), direction);
                }
            });

            // **嘗試將剩餘魔力輸出到相鄰方塊**
            neighborBlockEntity.getCapability(ModCapabilities.MANA, direction.getOpposite()).ifPresent(neighborManaStorage -> {
                if (neighborManaStorage.canReceive()) {
                    int manaToTransfer = (int) Math.min(excessMana.get(), 50); // 每次最多輸出 50 魔力
                    int acceptedMana = neighborManaStorage.insertMana(manaToTransfer, ManaAction.EXECUTE);
                    manaStorage.extractMana(acceptedMana, ManaAction.EXECUTE); // 從自己的魔力儲存中提取
                    excessMana.addAndGet(-acceptedMana); // 更新剩餘魔力
                    NeoMagnaMod.LOGGER.debug("Transferred {} mana to {} at direction {}", acceptedMana, neighborBlockEntity.getBlockPos(), direction);
                }
            });
        }
//
//        // 如果仍有剩餘魔力，記錄日誌
//        if (excessMana.get() > 0) {
//            MagicalIndustryMod.LOGGER.debug("Remaining mana after output: {}", excessMana.get());
//        }
    }


//    private void outputCapability(Direction direction, BlockEntity neighborBlockEntity, Capability<?> capability, int amount, BiConsumer<Integer, Boolean> extractor) {
//        neighborBlockEntity.getCapability(capability, direction.getOpposite()).ifPresent(neighborStorage -> {
//            if (neighborStorage instanceof IConfigurableBlock configurableNeighborBlock && !configurableNeighborBlock.isOutput(direction.getOpposite())) {
//                if (capability == ForgeCapabilities.ENERGY) {
//                    int acceptedEnergy = ((IEnergyStorage) neighborStorage).receiveEnergy(amount, false);
//                    extractor.accept(acceptedEnergy, false);
//                } else if (capability == ModCapabilities.MANA) {
//                    int acceptedMana = ((IUnifiedManaHandler) neighborStorage).receiveMana(amount, ManaAction.get(false));
//                    extractor.accept(acceptedMana, ManaAction.get(false));
//                }
//            }
//        });
//    }
//


    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyFuelHandler.cast();
        } else if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyStorage.cast();
        } else if (cap == ManaCapability.MANA) {
            return lazyManaStorage.cast();
        }

        return super.getCapability(cap, side);
    }


    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        // 加載燃料庫存
        if (tag.contains("FuelInventory", Tag.TAG_COMPOUND)) {
            CompoundTag inventoryTag = tag.getCompound("FuelInventory");
            for (int i = 0; i < fuelHandler.getSlots(); i++) {
                if (inventoryTag.contains("Slot" + i)) {
                    ItemStack stack = ItemStack.of(inventoryTag.getCompound("Slot" + i));
                    fuelHandler.setStackInSlot(i, stack);
                }
            }
        }

        // 加載方向配置
        if (tag.contains("DirectionConfig", Tag.TAG_COMPOUND)) {
            CompoundTag directionTag = tag.getCompound("DirectionConfig");
            for (Direction direction : Direction.values()) {
                this.directionConfig.put(direction, directionTag.getBoolean(direction.getName()));
            }
        }

        // 加載其他屬性
        manaStorage.setMana(tag.getInt("ManaStored"));
        energyStorage.receiveEnergy(tag.getInt("EnergyStored"), false);
        isWorking = tag.getBoolean("IsWorking");
        currentMode = tag.getInt("CurrentMode") == 0 ? Mode.MANA : Mode.ENERGY;
        burnTime = tag.getInt("BurnTime");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        // 保存燃料庫存
        CompoundTag inventoryTag = new CompoundTag();
        for (int i = 0; i < fuelHandler.getSlots(); i++) {
            ItemStack stack = fuelHandler.getStackInSlot(i);
            inventoryTag.put("Slot" + i, stack.save(new CompoundTag()));
        }
        tag.put("FuelInventory", inventoryTag);

        // 保存方向配置
        CompoundTag directionTag = new CompoundTag();
        for (Direction direction : Direction.values()) {
            directionTag.putBoolean(direction.getName(), this.directionConfig.getOrDefault(direction, false));
        }
        tag.put("DirectionConfig", directionTag);

        // 保存其他屬性
        tag.putInt("ManaStored", manaStorage.getMana());
        tag.putInt("EnergyStored", energyStorage.getEnergyStored());
        tag.putBoolean("IsWorking", isWorking);
        tag.putInt("CurrentMode", currentMode == Mode.MANA ? 0 : 1);
        tag.putInt("BurnTime", burnTime);
    }


    @Override
    public AABB getRenderBoundingBox() {
        // 將 Y 軸渲染範圍從當前位置擴展到高出 2 個方塊
        return new AABB(
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 3, worldPosition.getZ() + 1
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magical_industry.mana_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ManaGeneratorMenu(id, inv, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "mana_generator_controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        if (this.isWorking) {
            tAnimationState.getController().setAnimation(WORKING_ANIM);
        } else {
            tAnimationState.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }

    public boolean  getWorkingStatus() {
        return isWorking;
    }

    public void setWorking(boolean isWorking) {
        this.isWorking = isWorking;
        this.setChanged(); // 標記數據已更新

        if (this.level != null) {
            BlockState currentState = this.level.getBlockState(this.worldPosition);
            if (currentState.hasProperty(ManaGeneratorBlock.WORKING)) {
                BlockState newState = currentState.setValue(ManaGeneratorBlock.WORKING, isWorking);
                this.level.setBlock(this.worldPosition, newState, 3); // 更新方塊狀態
                this.level.sendBlockUpdated(this.worldPosition, currentState, newState, 3); // 同步到客戶端
            }
        }
    }


    public enum Mode {
        MANA,
        ENERGY
    }


    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag);
    }
}
