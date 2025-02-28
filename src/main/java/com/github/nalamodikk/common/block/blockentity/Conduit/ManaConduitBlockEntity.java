package com.github.nalamodikk.common.block.blockentity.Conduit;

import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;

import java.util.EnumMap;
import java.util.Map;

public class ManaConduitBlockEntity extends BlockEntity {
    private final Map<Direction, Boolean> outputDirections = new EnumMap<>(Direction.class);

    private final ManaStorage manaStorage;
    private LazyOptional<IUnifiedManaHandler> manaHandler;

    public ManaConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDUIT_BE.get(), pos, state);
        this.manaStorage = new ManaStorage(5000); // 設定導管的最大容量
        this.manaHandler = LazyOptional.of(() -> this.manaStorage);
    }



    /**
     * 嘗試從其他相鄰導管或設備吸收魔力
     */
    public void pullMana() {
        if (level == null || level.isClientSide) return;

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor != null) {
                LazyOptional<IUnifiedManaHandler> cap = neighbor.getCapability(ModCapabilities.MANA, direction.getOpposite());
                cap.ifPresent(handler -> {
                    int extracted = handler.extractMana(50, ManaAction.get(false));
                    manaStorage.insertMana(extracted, ManaAction.get(false));
                });
            }
        }
    }

    /**
     * 嘗試將魔力傳輸到相鄰設備
     */
    public void pushMana() {
        if (level == null || level.isClientSide) return;

        for (Direction direction : Direction.values()) {
            if (!outputDirections.getOrDefault(direction, false)) {
                continue; // 如果這個方向沒有開啟輸出，就跳過
            }

            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor != null) {
                LazyOptional<IUnifiedManaHandler> cap = neighbor.getCapability(ModCapabilities.MANA, direction.getOpposite());
                cap.ifPresent(handler -> {
                    int extracted = manaStorage.extractMana(50, ManaAction.get(false));
                    handler.insertMana(extracted, ManaAction.get(false));
                });
            }
        }
    }


    public ManaStorage getManaStorage() {
        return manaStorage;
    }

    public Map<Direction, Boolean> getOutputDirections() {
        return outputDirections;
    }


    public void toggleOutput (Direction direction){
        boolean  current = outputDirections.getOrDefault(direction,false);
        outputDirections.put(direction, !current);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        manaStorage.deserializeNBT(tag.getCompound("Mana"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (Map.Entry<Direction, Boolean> entry : outputDirections.entrySet()) {
            CompoundTag dirTag = new CompoundTag();
            dirTag.putString("Direction", entry.getKey().name());
            dirTag.putBoolean("Enabled", entry.getValue());
            list.add(dirTag);
        }
        tag.put("OutputDirections", list);
    }


    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ModCapabilities.MANA) {
            return manaHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        manaHandler.invalidate();
    }
}
