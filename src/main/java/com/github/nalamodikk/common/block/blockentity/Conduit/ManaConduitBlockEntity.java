package com.github.nalamodikk.common.block.blockentity.Conduit;

import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.network.mana_net.ManaNetworkManager;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * 魔力導管方塊實體，用於處理魔力儲存、傳輸邏輯。
 */
public class ManaConduitBlockEntity extends BlockEntity implements IUnifiedManaHandler {

    private final Set<Direction> outputDirections = EnumSet.noneOf(Direction.class); // 儲存輸出方向
    private static final int BASE_TRANSFER_RATE = 50; // 每 tick 傳輸的魔力量
    private final Set<BlockPos> checkedPositions = new HashSet<>();
    private int manaStored = 0; // 當前魔力存儲
    private static final int MAX_MANA = 1000; // 最大魔力存儲量

    public ManaConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDUIT_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            // 註冊到 ManaNetwork
            ManaNetworkManager.getInstance(level).registerConduit(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!level.isClientSide) {
            // 從 ManaNetwork 中移除
            ManaNetworkManager.getInstance(level).removeConduit(worldPosition);
        }
    }

    /**
     * 設置給定方向為輸出或輸入。
     */
    public void setOutput(Direction direction, boolean isOutput) {
        if (isOutput) {
            outputDirections.add(direction);
        } else {
            outputDirections.remove(direction);
        }
        setChanged(); // 標記數據變更
        updateClient(); // 同步客戶端
    }

    /**
     * 讓導管可以接收並傳輸魔力。
     */
    public void transferMana() {
        if (level == null || level.isClientSide || manaStored <= 0) return;

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(direction);
            BlockEntity neighborEntity = level.getBlockEntity(neighborPos);

            if (neighborEntity != null) {
                LazyOptional<IUnifiedManaHandler> manaOpt = neighborEntity.getCapability(ModCapabilities.MANA, direction.getOpposite());

                if (!manaOpt.isPresent()) {
                    // 嘗試獲取 ManaStorage
                    LazyOptional<IUnifiedManaHandler> manaStorageOpt = neighborEntity.getCapability(ManaCapability.MANA, direction.getOpposite());
                    manaStorageOpt.ifPresent(neighborManaStorage -> {
                        int manaToTransfer = Math.min(BASE_TRANSFER_RATE, neighborManaStorage.getNeededMana(0));
                        if (manaToTransfer > 0) {
                            int accepted = neighborManaStorage.insertMana(0, manaToTransfer, ManaAction.EXECUTE);
                            manaStored -= accepted;
                            setChanged();
                            updateClient();
                            MagicalIndustryMod.LOGGER.debug("if one :ManaConduit transferred {} mana to {}", accepted, neighborPos);
                        }
                    });
                } else {
                    manaOpt.ifPresent(neighborManaStorage -> {
                        int manaToTransfer = Math.min(BASE_TRANSFER_RATE, neighborManaStorage.getNeededMana(0));
                        if (manaToTransfer > 0) {
                            int accepted = neighborManaStorage.insertMana(0, manaToTransfer, ManaAction.EXECUTE);
                            manaStored -= accepted;
                            setChanged();
                            updateClient();
                            MagicalIndustryMod.LOGGER.debug("if two :ManaConduit transferred {} mana to {}", accepted, neighborPos);
                        }
                    });
                }
            }
        }
    }

    /**
     * 獲取魔力 Capability。
     */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ModCapabilities.MANA) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(cap, side);
    }

    /**
     * 更新客戶端數據。
     */
    private void updateClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public int getMana() {
        return manaStored;
    }

    @Override
    public void addMana(int amount, ManaAction action) {
        if (action.execute()) {
            manaStored = Math.min(manaStored + amount, MAX_MANA);
            setChanged();
            updateClient();
        }
    }

    @Override
    public void consumeMana(int amount) {
        manaStored = Math.max(manaStored - amount, 0);
        setChanged();
        updateClient();
    }

    @Override
    public void setMana(int amount) {
        manaStored = Math.min(Math.max(amount, 0), MAX_MANA);
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
        return MAX_MANA;
    }

    @Override
    public int getManaContainerCount() {
        return 1;
    }

    @Override
    public int getMana(int container) {
        return manaStored;
    }

    @Override
    public void setMana(int container, int mana) {
        this.manaStored = Math.min(Math.max(mana, 0), MAX_MANA);
        setChanged();
        updateClient();
    }

    @Override
    public int getMaxMana(int container) {
        return MAX_MANA;
    }

    @Override
    public int getNeededMana(int container) {
        return MAX_MANA - manaStored;
    }

    @Override
    public int insertMana(int container, int amount, ManaAction action) {
        int manaToInsert = Math.min(amount, getNeededMana(container));
        if (action.execute()) {
            manaStored += manaToInsert;
            setChanged();
            updateClient();
        }
        return manaToInsert;
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        int manaToExtract = Math.min(amount, manaStored);
        if (action.execute()) {
            manaStored -= manaToExtract;
            setChanged();
            updateClient();
        }
        return manaToExtract;
    }
}
