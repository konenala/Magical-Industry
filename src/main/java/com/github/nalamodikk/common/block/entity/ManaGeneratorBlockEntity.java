package com.github.nalamodikk.common.block.entity;

import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.datagen.ManaGenerationRateLoader;
import com.github.nalamodikk.common.screen.ManaGenerator.ManaGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

public class ManaGeneratorBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {

    private final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            markUpdated(); // 標記方塊需要更新
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (currentMode == Mode.ENERGY) {
                return getBurnTime(stack) > 0; // 在能量模式下，只允許 Forge 燃料系統中的燃料
            } else if (currentMode == Mode.MANA) {
                return stack.is(ItemTags.create(new net.minecraft.resources.ResourceLocation("magical_industry", "mana"))); // 只允許具有 mana 標籤的物品
            }
            return false;
        }

        private int getBurnTime(@NotNull ItemStack stack) {
            int burnTime = net.minecraftforge.common.ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
            return burnTime > 0 ? burnTime : 200; // 默認至少有 200 個遊戲刻（相當於 10 秒）
        }


    };

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final ContainerData containerData;
    private final EnergyStorage energyStorage = new EnergyStorage(10000);
    private final ManaStorage manaStorage = new ManaStorage(10000);
    private boolean isWorking = false;

    public static final int MAX_MANA = 10000;
    private static final int MANA_GENERATION_RATE = 20;
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("working");
    private Mode currentMode = Mode.MANA; // 默認模式為生成魔力
    private int burnTime; // 當前燃料的剩餘燃燒時間
    private int currentBurnTime; // 當前燃料的總燃燒時間

    private int getBurnTime(ItemStack stack) {
        // 返回特定物品的燃燒時間，這裡使用 Forge 的燃料系統
        return net.minecraftforge.common.ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    public int getCurrentMode() {
        return this.currentMode == Mode.MANA ? 0 : 1;
    }



    private enum Mode {
        MANA,
        ENERGY
    }

    public ManaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_GENERATOR_BE.get(), pos, state);
//        MagicalIndustryMod.LOGGER.info("ManaGeneratorBlockEntity created at " + pos);

        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> manaStorage.getMana();
                    case 1 -> energyStorage.getEnergyStored();
                    case 2 -> currentMode == Mode.MANA ? 0 : 1;
                    case 3 -> isWorking ? 1 : 0;
                    case 4 -> burnTime; // 當前燃燒時間
                    case 5 -> currentBurnTime; // 總燃燒時間
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // 確保只在伺服器端設置數據
                if (level != null && !level.isClientSide()) {
                    switch (index) {
                        case 0 -> manaStorage.setMana(value);
                        case 1 -> energyStorage.receiveEnergy(value, false);
                        case 2 -> currentMode = (value == 0) ? Mode.MANA : Mode.ENERGY;
                        case 3 -> isWorking = (value == 1);
                        case 4 -> burnTime = value; // 設置當前燃燒時間
                        case 5 -> currentBurnTime = value; // 設置總燃燒時間
                    }
                }
            }

            @Override
            public int getCount() {
                return 6; // manaStored, energyStored, currentMode, isWorking, burnTime, currentBurnTime
            }
        };


    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magical_industry.mana_generator");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "mana_generator_controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        if (this.isWorking) {
            tAnimationState.getController().setAnimation(WORKING_ANIM);
        } else {
            tAnimationState.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public double getTick(Object o) {
        return RenderUtils.getCurrentTick();
    }

    private int tickCounter = 0; // 計數器，用於控制燃燒速率

    //  下面這是mana生成速率
    private int getManaGenerationRateForFuel(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ManaGenerationRateLoader.MANA_GENERATION_RATES.getOrDefault(itemId, 0);
    }

    //  下面這是能量(發電)生成速率
    private int getEnergyGenerationRateForFuel(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ManaGenerationRateLoader.ENERGY_GENERATION_RATES.getOrDefault(itemId, 0);
    }


    public static void tick(Level level, BlockPos pos, BlockState state, ManaGeneratorBlockEntity blockEntity) {
        if (!level.isClientSide) {
            if (blockEntity.hasFuel()) {
                // 如果燃燒時間為 0，意味著需要消耗一個新的燃料
                if (blockEntity.burnTime <= 0) {
                    ItemStack fuel = blockEntity.fuelHandler.extractItem(0, 1, false);
                    blockEntity.currentBurnTime = blockEntity.getBurnTime(fuel);
                    blockEntity.burnTime = blockEntity.currentBurnTime;

                    // 在燃燒時間重置後才生成能量或魔力
                    if (blockEntity.currentBurnTime > 0) {
                        if (blockEntity.currentMode == Mode.MANA) {
                            blockEntity.manaStorage.addMana(blockEntity.getManaGenerationRateForFuel(fuel));
                        } else if (blockEntity.currentMode == Mode.ENERGY) {
                            blockEntity.energyStorage.receiveEnergy(blockEntity.getEnergyGenerationRateForFuel(fuel), false);
                        }
                    }
                    blockEntity.setChanged();
                }

                // 如果還有剩餘燃燒時間，減少燃燒時間
                if (blockEntity.burnTime > 0) {
                    blockEntity.burnTime--;
                    blockEntity.isWorking = true;
                } else {
                    blockEntity.isWorking = false;
                }

                blockEntity.markUpdated();
            } else {
                blockEntity.isWorking = false;
                blockEntity.markUpdated();
            }
        }
    }






    private boolean hasFuel() {
        return !fuelHandler.getStackInSlot(0).isEmpty();
    }

    public void markUpdated() {
        if (this.level != null) {
            this.level.setBlock(this.worldPosition, this.getBlockState(), 3);
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new ManaGeneratorMenu(
                pContainerId,
                pPlayerInventory,
                this.worldPosition,
                this.level,
                this.fuelHandler
        );
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.of(() -> fuelHandler).cast();
        } else if (cap == ModCapabilities.MANA) {
            return LazyOptional.of(() -> manaStorage).cast();
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
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ManaStored", this.manaStorage.getMana());
        tag.putInt("EnergyStored", this.energyStorage.getEnergyStored());
        tag.putBoolean("IsWorking", this.isWorking);
        tag.putInt("CurrentMode", this.currentMode == Mode.MANA ? 0 : 1);
    }

    public ContainerData getContainerData() {
        return this.containerData;
    }


    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }


    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag); // 保存最新狀態
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag); // 加載最新狀態
    }


    public ItemStackHandler getInventory() {
        return fuelHandler;
    }

    public void toggleMode() {
        this.currentMode = (this.currentMode == Mode.MANA) ? Mode.ENERGY : Mode.MANA;
        this.isWorking = false; // 重置工作狀態
        this.setChanged(); // 標記狀態變更

        if (!level.isClientSide) {
            // 通知客戶端這個方塊已經變更
            level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            MagicalIndustryMod.LOGGER.info("Mode successfully toggled to: " + this.currentMode + " for block at: " + this.worldPosition);
        }

        markUpdated();
    }




}



