package com.github.nalamodikk.common.block.entity;

import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.ManaGenerator.AbstractManaGenerator;
import com.github.nalamodikk.common.screen.ManaGenerator.ManaGeneratorMenu;
import com.github.nalamodikk.common.util.loader.FuelRateLoader;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Block;
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

public class ManaGeneratorBlockEntity extends BlockEntity implements GeoBlockEntity, GeoAnimatable, MenuProvider {
    public static final int MAX_MANA = 10000; // 或者保留 private，然後新增 getter
    public static int getMaxMana() {
        return MAX_MANA;
    }

    private static final int MAX_ENERGY = 10000;

    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY);
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

                // 檢查是否有自定義的燃燒時間，或是否為標準的燃燒燃料
                return (fuelRate != null && fuelRate.getBurnTime() > 0) ||
                        (ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0);
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
    private int manaRate = 0;
    private int energyRate = 10; // 默認能量產生速率
    private int baseManaRate = 5; // 默認魔力生成速率
    private int baseEnergyRate = 10;
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("working");

    public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(fuelHandler.getSlots());
        for (int i = 0; i < fuelHandler.getSlots(); i++) {
            inventory.setItem(i, fuelHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // 定義燃燒時間邏輯
    private int getBurnTime(@NotNull ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        FuelRateLoader.FuelRate fuelRate = FuelRateLoader.getFuelRateForItem(itemId);
        return fuelRate != null ? Math.max(fuelRate.getManaRate(), 0) : 0;
    }

    public ItemStackHandler getInventory() {
        return this.fuelHandler; // 返回你的 `ItemStackHandler`
    }

    public int getCurrentMode() {
        return this.currentMode == Mode.MANA ? 0 : 1; // 使用你的 Mode enum
    }

    // 更新狀態標記
    public void markUpdated() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
    public ContainerData getContainerData() {
        return containerData;
    }


    public static void tick(Level level, BlockPos pos, BlockState state, ManaGeneratorBlockEntity blockEntity) {
        if (!level.isClientSide) {
            // 如果當前燃燒時間為 0，則嘗試從燃料槽中加載新的燃料
            if (blockEntity.burnTime <= 0) {
                // 檢查燃料槽中是否有可用燃料且魔力槽未滿
                if (blockEntity.hasFuel() && !blockEntity.manaStorage.isFull()) {
                    ItemStack fuel = blockEntity.fuelHandler.getStackInSlot(0);
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(fuel.getItem());
                    FuelRateLoader.FuelRate fuelRate = FuelRateLoader.getFuelRateForItem(itemId);

                    int burnTime = 0;

                    // 確認這個物品是否是自定義燃料
                    if (fuelRate != null && fuelRate.getBurnTime() > 0) {
                        burnTime = fuelRate.getBurnTime();
                        blockEntity.manaRate = fuelRate.getManaRate();
                        blockEntity.energyRate = fuelRate.getEnergyRate();
                    } else {
                        // 如果不是自定義燃料，檢查是否是 Forge 支持的燃料
                        burnTime = net.minecraftforge.common.ForgeHooks.getBurnTime(fuel, null);
                        if (burnTime > 0) {
                            blockEntity.manaRate = 0; // 在能量模式下，Mana 生成率為 0
                            blockEntity.energyRate = burnTime; // 將燃燒時間作為能量生產速率
                        }
                    }

                    // 如果找到了有效的燃料，開始燃燒
                    if (burnTime > 0) {
                        blockEntity.fuelHandler.extractItem(0, 1, false);  // 提取燃料
                        blockEntity.currentBurnTime = burnTime;
                        blockEntity.burnTime = blockEntity.currentBurnTime;

                        // 燃燒開始後立即生成一次魔力或能量
                        if (blockEntity.currentMode == Mode.MANA && fuelRate != null) {
                            blockEntity.manaStorage.addMana(blockEntity.manaRate);
                        } else if (blockEntity.currentMode == Mode.ENERGY) {
                            blockEntity.energyStorage.receiveEnergy(blockEntity.energyRate, false);
                        }

                        blockEntity.isWorking = true; // 設置工作狀態為 true
                        blockEntity.markUpdated();
                    } else {
                        blockEntity.isWorking = false; // 沒有有效燃料時設置工作狀態為 false
                    }
                } else {
                    blockEntity.isWorking = false; // 沒有燃料或魔力槽已滿
                }
            } else {
                // 如果燃燒時間大於 0，則進行倒計時
                blockEntity.burnTime--;
                // 在燃燒時間內不生成魔力或能量，因為我們只在燃燒開始時生成一次
                blockEntity.isWorking = true; // 設置工作狀態為 true
            }

            // 如果燃燒時間結束且無法繼續加載燃料，設置工作狀態為 false
            if (blockEntity.burnTime <= 0) {
                blockEntity.isWorking = false;
            }

            // 標記更新方塊狀態
            blockEntity.markUpdated();
        }
    }



    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0:
                    return manaStorage.getMana();
                case 1:
                    return energyStorage.getEnergyStored();
                case 2:
                    return currentMode == Mode.MANA ? 0 : 1;
                case 3:
                    return burnTime;
                case 4:
                    return currentBurnTime;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0:
                    manaStorage.setMana(value);
                    break;
                case 1:
                    energyStorage.receiveEnergy(value, false);
                    break;
                case 2:
                    currentMode = value == 0 ? Mode.MANA : Mode.ENERGY;
                    break;
                case 3:
                    burnTime = value;
                    break;
                case 4:
                    currentBurnTime = value;
                    break;
            }
        }

        @Override
        public int getCount() {
            return 5; // 根據你需要同步的數據數量
        }
    };


    // 判斷是否有燃料
    private boolean hasFuel() {
        return !fuelHandler.getStackInSlot(0).isEmpty();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.of(() -> fuelHandler).cast();
        } else if (cap == ForgeCapabilities.ENERGY) {
            return LazyOptional.of(() -> energyStorage).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.manaStorage.setMana(tag.getInt("ManaStored"));
        this.energyStorage.receiveEnergy(tag.getInt("EnergyStored"), false);
        this.isWorking = tag.getBoolean("IsWorking");
        this.currentMode = tag.getInt("CurrentMode") == 0 ? Mode.MANA : Mode.ENERGY;
        this.burnTime = tag.getInt("BurnTime");
        this.currentBurnTime = tag.getInt("CurrentBurnTime");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ManaStored", this.manaStorage.getMana());
        tag.putInt("EnergyStored", this.energyStorage.getEnergyStored());
        tag.putBoolean("IsWorking", this.isWorking);
        tag.putInt("CurrentMode", this.currentMode == Mode.MANA ? 0 : 1);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("CurrentBurnTime", this.currentBurnTime);
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



    public void toggleMode() {
        this.currentMode = this.currentMode == Mode.MANA ? Mode.ENERGY : Mode.MANA;
        this.isWorking = false;
        markUpdated();
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

    public enum Mode {
        MANA,
        ENERGY
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaGeneratorBlockEntity blockEntity) {
        blockEntity.tick(level, pos, state, blockEntity);
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
