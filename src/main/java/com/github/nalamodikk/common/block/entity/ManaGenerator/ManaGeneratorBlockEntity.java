package com.github.nalamodikk.common.block.entity.ManaGenerator;

import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.ModBlockEntities;
import com.github.nalamodikk.common.mana.NalaEnergyStorage;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
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

public class ManaGeneratorBlockEntity extends AbstractManaGenerator implements GeoBlockEntity, GeoAnimatable, MenuProvider {
    public static final int MAX_MANA = 10000;
    public static int getMaxMana() {
        return MAX_MANA;
    }

    private static final int MAX_ENERGY = 10000;

    private final NalaEnergyStorage energyStorage = new NalaEnergyStorage(MAX_ENERGY);
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
    private Mode currentMode = Mode.MANA;
    private int burnTime;
    private int currentBurnTime;
    private int burnTimeMana = 0;
    private int burnTimeEnergy = 0;
    private int manaRate = 0;
    private int energyRate = 10;
    private int manaProductionRate; // 初始化魔力生產速率
    private int energyProductionRate; // 初始化能量生產速率


    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("working");

    public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state, MAX_ENERGY, MAX_MANA);
    }

    @Override
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(fuelHandler.getSlots());
        for (int i = 0; i < fuelHandler.getSlots(); i++) {
            inventory.setItem(i, fuelHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public ItemStackHandler getInventory() {
        return this.fuelHandler; // 返回你的 `ItemStackHandler`
    }

    public int getCurrentMode() {
        return this.currentMode == Mode.MANA ? 0 : 1; // 使用你的 Mode enum
    }

    @Override
    public boolean hasFuel() {
        return !fuelHandler.getStackInSlot(0).isEmpty();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ManaGeneratorBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.commonTick();
        }
    }

    protected void commonTick() {
        if (!isWorking && hasFuel()) {
            loadFuel(); // 確保加載燃料
        }

        if (currentMode == Mode.MANA && burnTimeMana > 0) {
            burnTimeMana--;
            if (burnTimeMana == 0) {
                // 燃燒完成，生成魔力
                manaStorage.addMana(manaProductionRate);
                isWorking = false; // 完成生成
            }
        } else if (currentMode == Mode.ENERGY && burnTimeEnergy > 0) {
            burnTimeEnergy--;
            if (burnTimeEnergy == 0) {
                // 燃燒完成，生成能量
                energyStorage.receiveEnergy(energyProductionRate, false);
                isWorking = false; // 完成生成
            }
        }

        markUpdated(); // 更新狀態
    }

    @Override
    protected void loadFuel() {
        if (hasFuel()) {
            ItemStack fuel = fuelHandler.getStackInSlot(0);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(fuel.getItem());
            FuelRateLoader.FuelRate fuelRate = FuelRateLoader.getFuelRateForItem(itemId);

            if (currentMode == Mode.MANA) {
                if (fuelRate != null && fuelRate.getBurnTime() > 0) {
                    // 如果 JSON 中有定義的燃燒時間和魔力生成量
                    burnTimeMana = fuelRate.getBurnTime();
                    manaProductionRate = fuelRate.getManaRate();
                    fuelHandler.extractItem(0, 1, false);
                    isWorking = true;
                }
            } else if (currentMode == Mode.ENERGY) {
                if (fuelRate != null && fuelRate.getBurnTime() > 0) {
                    // 如果 JSON 中有定義的燃燒時間和能量生成速率
                    burnTimeEnergy = fuelRate.getBurnTime();
                    energyProductionRate = fuelRate.getEnergyRate();
                } else {
                    // 如果沒有自定義的燃燒時間，則使用 Forge 的燃燒時間
                    int forgeBurnTime = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
                    if (forgeBurnTime > 0) {
                        burnTimeEnergy = forgeBurnTime;
                        energyProductionRate = calculateEnergyProductionRate(fuel);
                    }
                }

                if (burnTimeEnergy > 0) {
                    fuelHandler.extractItem(0, 1, false);
                    isWorking = true;
                }
            }
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

    @Override
    public ContainerData getContainerData() {
        return containerData;
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
        if (tag == null) {
            tag = new CompoundTag(); // 如果為 null，創建一個新的 CompoundTag
        }
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag == null) {
            return; // 如果 tag 為 null，直接返回
        }
        super.handleUpdateTag(tag);
        load(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ManaStored", this.manaStorage.getMana());
        tag.putInt("EnergyStored", this.energyStorage.getEnergyStored());
        tag.putBoolean("IsWorking", this.isWorking);
        tag.putInt("CurrentMode", this.currentMode == Mode.MANA ? 0 : 1);
        tag.putInt("BurnTimeMana", this.burnTimeMana);
        tag.putInt("BurnTimeEnergy", this.burnTimeEnergy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.manaStorage.setMana(tag.getInt("ManaStored"));
        this.energyStorage.receiveEnergy(tag.getInt("EnergyStored"), false);
        this.isWorking = tag.getBoolean("IsWorking");
        this.currentMode = tag.getInt("CurrentMode") == 0 ? Mode.MANA : Mode.ENERGY;
        this.burnTimeMana = tag.getInt("BurnTimeMana");
        this.burnTimeEnergy = tag.getInt("BurnTimeEnergy");
    }

    public void toggleMode() {
        // 切換模式
        this.currentMode = this.currentMode == Mode.MANA ? Mode.ENERGY : Mode.MANA;

        if (this.currentMode == Mode.MANA) {
            // 切換到 MANA 模式，保留魔力燃燒進度並重置能量燃燒狀態
            this.burnTimeEnergy = 0;
            this.energyRate = 0; // 停止能量生成
            if (burnTimeMana > 0) {
                this.isWorking = true; // 保持魔力工作狀態
            }
        } else if (this.currentMode == Mode.ENERGY) {
            // 切換到 ENERGY 模式，保留能量燃燒進度並重置魔力燃燒狀態
            this.burnTimeMana = 0;
            this.manaRate = 0; // 停止魔力生成
            if (burnTimeEnergy > 0) {
                this.isWorking = true; // 保持能量工作狀態
            }
        }

        // 更新狀態
        markUpdated();
    }
}
