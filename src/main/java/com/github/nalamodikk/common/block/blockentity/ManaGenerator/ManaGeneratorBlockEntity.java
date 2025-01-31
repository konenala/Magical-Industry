package com.github.nalamodikk.common.block.blockentity.ManaGenerator;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.block.ManaGenerator.ManaGeneratorBlock;
import com.github.nalamodikk.common.network.mana_net.NetworkManager;
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

public class ManaGeneratorBlockEntity extends BlockEntity implements GeoBlockEntity, GeoAnimatable, MenuProvider , IConfigurableBlock {

    public static final int MANA_STORED_INDEX = 0;
    public static final int ENERGY_STORED_INDEX = 1;
    public static final int MODE_INDEX = 2;
    public static final int BURN_TIME_INDEX = 3;
    public static final int CURRENT_BURN_TIME_INDEX = 4;
    private final UnifiedSyncManager syncManager = new UnifiedSyncManager(5); // 假設有 5 個需要同步的數據



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
                return stack.is(ItemTags.create(new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana"))); // 魔力模式只允許標籤為 mana 的物品
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


//    //已棄用方法

//    private final ContainerData containerData = new ContainerData() {
//        @Override
//        public int get(int index) {
//            return switch (index) {
//                case MANA_STORED_INDEX -> manaStorage.getMana();
//                case ENERGY_STORED_INDEX ->  energyStorage.getEnergyStored();
//                case MODE_INDEX -> currentMode == Mode.MANA ? 0 : 1;
//                case BURN_TIME_INDEX -> burnTime;
//                case CURRENT_BURN_TIME_INDEX -> currentBurnTime;
//                default -> 0;
//            };
//        }
//
//        @Override
//        public void set(int index, int value) {
//            switch (index) {
//                case MANA_STORED_INDEX -> manaStorage.setMana(value);
//                case ENERGY_STORED_INDEX -> energyStorage.setEnergy(BigDecimal.valueOf(value)); // 確保能量的更新是同步進行的
//                case MODE_INDEX -> currentMode = (value == 0) ? Mode.MANA : Mode.ENERGY;
//                case BURN_TIME_INDEX -> burnTime = value;
//                case CURRENT_BURN_TIME_INDEX -> currentBurnTime = value;
//            }
//            setChanged(); // 確保標記數據變更
//        }
//
//        @Override
//        public int getCount() {
//            return DATA_COUNT;
//        }
//    };




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
            long generatedMana = blockEntity.generateEnergyOrMana(); // 生成魔力
            long excessMana = NetworkManager.getInstance(level).insertMana(pos, generatedMana); // 插入網路
            blockEntity.manaStorage.insertMana((int) excessMana, ManaAction.EXECUTE); // 儲存剩餘魔力
            blockEntity.outputEnergyAndMana(); // 輸出魔力或能量
            blockEntity.markUpdated(); // 標記數據更新
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        NetworkManager.onConduitPlaced(this.level, this.worldPosition);
    }





    private void sync() {
        if (level != null && !level.isClientSide()) {
            // 將能量和魔力的狀態同步到 UnifiedSyncManager 中
            syncManager.set(ENERGY_STORED_INDEX, energyStorage.getEnergyStored());
            syncManager.set(MANA_STORED_INDEX, manaStorage.getMana());
            syncManager.set(MODE_INDEX, currentMode == Mode.MANA ? 0 : 1);
            syncManager.set(BURN_TIME_INDEX, burnTime);
            syncManager.set(CURRENT_BURN_TIME_INDEX, currentBurnTime);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            setChanged();

        }
    }

    private long insertIntoNetwork(Level level, BlockPos pos, long mana) {
        if (mana > 0) {
            NetworkManager manager = NetworkManager.getInstance(level); // 獲取 NetworkManager 實例
            return manager.insertMana(pos, mana); // 使用實例方法
        }
        return mana; // 如果沒有成功插入，返回原始 mana
    }

    private void initializeBurn(ItemStack fuel) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(fuel.getItem());

        // 根據模式選擇燃燒時間
        if (currentMode == Mode.ENERGY) {
            burnTime = BurnTimeFuelLoader.getBurnTime(itemId);
        } else if (currentMode == Mode.MANA) {
            burnTime = ManaFuelLoader.getManaFuelValue(itemId);
        }

        if (burnTime > 0) {
            fuelHandler.extractItem(0, 1, false); // 消耗燃料
            setWorking(true); // 設置為工作狀態
        }
    }



    private long generateEnergyOrMana() {
        long generatedAmount = 0; // 記錄生成的能量或魔力

        ItemStack fuel = fuelHandler.getStackInSlot(0);

        // 如果燃料槽為空且燃燒時間耗盡，停止工作
        if (fuel.isEmpty() && burnTime <= 0) {
            isWorking = false;
            return generatedAmount; // 返回 0
        }

        // 如果燃燒時間耗盡，且有燃料，並且能量/魔力未滿，開始新一輪燃燒
        if (burnTime <= 0 && !fuel.isEmpty()) {
            if (isStorageFull()) {
                setWorking(false); // 停止工作
                return generatedAmount; // 返回 0
            }

            initializeBurn(fuel); // 初始化燃燒
        }

        // 如果還在燃燒，則執行生成邏輯
        if (burnTime > 0) {
            burnTime--;

            // 燃燒結束時停止工作
            if (burnTime == 0) {
                setWorking(false);
            }

            // 根據當前模式生成能量或魔力
            if (currentMode == Mode.ENERGY) {
                generatedAmount = generateEnergy();
            } else if (currentMode == Mode.MANA) {
                generatedAmount = generateMana();
            }
        }

        return generatedAmount;
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

    // 檢查存儲是否已滿
    private boolean isStorageFull() {
        return (currentMode == Mode.ENERGY && energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) ||
                (currentMode == Mode.MANA && manaStorage.getMana() >= manaStorage.getMaxMana());
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
            manaStorage.addMana((int) generatedMana);
        }

        return generatedMana;
    }

// ==========================================================================


    private void outputEnergyAndMana() {
        for (Direction direction : Direction.values()) {
            // 只在設置為「輸出」的方向上進行能量和魔力的輸出
            if (isOutput(direction)) {
                BlockEntity neighborBlockEntity = level.getBlockEntity(worldPosition.relative(direction));
                if (neighborBlockEntity != null) {
                    // 嘗試將能量輸出到相鄰方塊
                    neighborBlockEntity.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(neighborEnergyStorage -> {
                        if (neighborEnergyStorage.canReceive() && neighborBlockEntity instanceof IConfigurableBlock configurableNeighborBlock) {
                            // 確認相鄰的方塊在這個方向上是「輸入」
                            if (!configurableNeighborBlock.isOutput(direction.getOpposite())) {
                                int energyToTransfer = Math.min(energyStorage.getEnergyStored(), 100);
                                int acceptedEnergy = neighborEnergyStorage.receiveEnergy(energyToTransfer, false);
                                energyStorage.extractEnergy(acceptedEnergy, false);
                            }
                        }
                    });

                    // 嘗試將魔力輸出到相鄰方塊
                    neighborBlockEntity.getCapability(ModCapabilities.MANA, direction.getOpposite()).ifPresent(neighborManaStorage -> {
                        if (neighborManaStorage.canReceive() && neighborBlockEntity instanceof IConfigurableBlock configurableNeighborBlock) {
                            // 確認相鄰的方塊在這個方向上是「輸入」
                            if (!configurableNeighborBlock.isOutput(direction.getOpposite())) {
                                int manaToTransfer = Math.min(manaStorage.getMana(), 50);
                                int acceptedMana = neighborManaStorage.receiveMana(manaToTransfer, ManaAction.get(false));
                                manaStorage.extractMana(acceptedMana, ManaAction.get(false));
                            }
                        }
                    });
                }
            }
        }
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
