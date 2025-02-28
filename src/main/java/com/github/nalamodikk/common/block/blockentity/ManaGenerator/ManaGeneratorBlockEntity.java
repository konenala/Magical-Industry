package com.github.nalamodikk.common.block.blockentity.ManaGenerator;

import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.register.ConfigManager;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.EnumMap;

public class ManaGeneratorBlockEntity extends BlockEntity implements GeoBlockEntity , MenuProvider {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("working");
    private static final int MAX_MANA = 30000; // 最大魔力儲存
    private static final int DEFAULT_BURN_TIME = 200; // 每次燃燒 200 tick
    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA);
    private final LazyOptional<ManaStorage> lazyManaStorage = LazyOptional.of(() -> manaStorage);

    private int burnTime; // 剩餘燃燒時間
    private boolean isWorking = false;
    private final EnumMap<Direction, Boolean> outputDirections = new EnumMap<>(Direction.class);

    public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state);
    }
    private final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }
    };
    private final LazyOptional<ItemStackHandler> lazyFuelHandler = LazyOptional.of(() -> fuelHandler);

    public void markUpdated() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        dropInventory();
    }

    /**
     * **當方塊被破壞時，掉落燃料槽內的物品**
     */
    private void dropInventory() {
        if (level == null || level.isClientSide) return;

        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, getFuelItem());

        Containers.dropContents(level, worldPosition, inventory);
    }

    /**
     * **獲取燃料槽內的物品**
     */
    private ItemStack getFuelItem() {
        // 這裡的 `fuelHandler` 代表你的燃料槽
        return fuelHandler.getStackInSlot(0).copy();
    }

    public void drops() {
        if (this.level == null || this.level.isClientSide) {
            return; // 只在伺服器端處理
        }

        SimpleContainer inventory = new SimpleContainer(fuelHandler.getSlots()); // 創建一個臨時物品容器
        for (int i = 0; i < fuelHandler.getSlots(); i++) {
            inventory.setItem(i, fuelHandler.getStackInSlot(i)); // 把物品放進容器
        }

        // 在方塊位置掉落所有物品
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
    /**
     * **🔥 核心邏輯 - Tick 運行**
     */
    public static void tick(Level level, BlockPos pos, BlockState state, ManaGeneratorBlockEntity blockEntity) {
        if (!level.isClientSide) {
            if (blockEntity.isWorking) {
                blockEntity.burnTime--;
                if (blockEntity.burnTime <= 0) {
                    blockEntity.isWorking = false;
                }
                blockEntity.generateMana();
            } else {
                blockEntity.tryConsumeFuel();
            }
            blockEntity.outputMana();
            blockEntity.setChanged();
        }
    }

    /**
     * **🔥 消耗燃料**
     */
    private void tryConsumeFuel() {
        if (burnTime > 0) return; // 燃燒時間還沒結束

        // 🔹 這裡可以擴展成讀取 JSON 燃料配置
        burnTime = DEFAULT_BURN_TIME;
        isWorking = true;
    }

    /**
     * **💨 產生魔力**
     */
    private void generateMana() {
        manaStorage.addMana(ConfigManager.COMMON.manaRate.get(), ManaAction.EXECUTE);
    }

    /**
     * **🔄 輸出魔力**
     */
    private void outputMana() {
        if (level == null) return;

        for (Direction direction : Direction.values()) {
            if (!isOutput(direction)) continue;

            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor != null) {
                neighbor.getCapability(ModCapabilities.MANA, direction.getOpposite()).ifPresent(handler -> {
                    int transfer = Math.min(manaStorage.getMana(), 50);
                    int accepted = handler.insertMana(transfer, ManaAction.EXECUTE);
                    manaStorage.extractMana(accepted, ManaAction.EXECUTE);
                });
            }
        }
    }

    /**
     * **🔄 是否輸出魔力**
     */
    public boolean isOutput(Direction direction) {
        return outputDirections.getOrDefault(direction, false);
    }

    /**
     * **📦 存檔**
     */
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BurnTime", burnTime);
        tag.putBoolean("IsWorking", isWorking);
        tag.putInt("ManaStored", manaStorage.getMana());
    }

    /**
     * **📂 讀檔**
     */
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime = tag.getInt("BurnTime");
        isWorking = tag.getBoolean("IsWorking");
        manaStorage.setMana(tag.getInt("ManaStored"));
    }

    /**
     * **⚡ 能力系統**
     */
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ManaCapability.MANA) {
            return lazyManaStorage.cast();
        }
        return super.getCapability(cap, side);
    }


    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        if (this.isWorking) {
            state.getController().setAnimation(WORKING_ANIM);
        } else {
            state.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }



    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "mana_generator_controller", 0, this::predicate));

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Component getDisplayName() {
        return null;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int p_39954_, Inventory p_39955_, Player p_39956_) {
        return null;
    }


}
