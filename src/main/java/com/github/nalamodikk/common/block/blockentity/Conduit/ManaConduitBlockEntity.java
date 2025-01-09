package com.github.nalamodikk.common.block.blockentity.Conduit;

import com.github.nalamodikk.common.network.ManaNetwork.ManaNetworkNode;
import com.github.nalamodikk.common.network.ManaNetwork.ManaNetworkManager;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.Capability.ModCapabilities;
import com.github.nalamodikk.common.block.block.Conduit.ManaConduitBlock;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ManaConduitBlockEntity extends BlockEntity {
    private final ManaStorage manaStorage = new ManaStorage(MAX_MANA);
    private VoxelShape cachedShape;
    private ManaNetworkNode node;
    private int manaStored = 0;
    private static final int MAX_MANA = 1000;

    public ManaConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDUIT_BE.get(), pos, state);
    }

    public int getManaStored() {
        return manaStored;
    }

    public int getMaxMana() {
        return MAX_MANA;
    }

    public VoxelShape getCachedShape(BlockState state) {
        if (cachedShape == null) {
            cachedShape = generateShape(state);
        }
        return cachedShape;
    }

    /**
     * 根據 BlockState 生成形狀
     */
    private VoxelShape generateShape(BlockState state) {
        VoxelShape shape = ManaConduitBlock.CENTER_SHAPE; // 基本中心部分
        if (state.getValue(ManaConduitBlock.NORTH)) shape = Shapes.or(shape, ManaConduitBlock.NORTH_SHAPE);
        if (state.getValue(ManaConduitBlock.EAST)) shape = Shapes.or(shape, ManaConduitBlock.EAST_SHAPE);
        if (state.getValue(ManaConduitBlock.SOUTH)) shape = Shapes.or(shape, ManaConduitBlock.SOUTH_SHAPE);
        if (state.getValue(ManaConduitBlock.WEST)) shape = Shapes.or(shape, ManaConduitBlock.WEST_SHAPE);
        if (state.getValue(ManaConduitBlock.UP)) shape = Shapes.or(shape, ManaConduitBlock.UP_SHAPE);
        if (state.getValue(ManaConduitBlock.DOWN)) shape = Shapes.or(shape, ManaConduitBlock.DOWN_SHAPE);
        return shape;
    }

    /**
     * 當連接更新時，清除緩存的形狀
     */
    public void invalidateShapeCache() {
        this.cachedShape = null;
    }
    public void addMana(int amount) {
        this.manaStored = Math.min(this.manaStored + amount, MAX_MANA);
    }

    public void consumeMana(int amount) {
        this.manaStored = Math.max(this.manaStored - amount, 0);
    }

    public void transferMana() {
        if (level == null || manaStored <= 0) return;

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor instanceof ManaConduitBlockEntity conduit && conduit.getManaStored() < conduit.getMaxMana()) {
                int transferAmount = Math.min(50, this.manaStored); // 每次傳輸 50 單位魔力
                conduit.addMana(transferAmount);
                this.consumeMana(transferAmount);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 確保 level 存在，並且邏輯僅在服務端執行
        if (level != null && !level.isClientSide) {
            node = new ManaNetworkNode(worldPosition, level);
            ManaNetworkManager.getInstance().registerNode(node);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // 確保 level 和 node 存在，並且邏輯僅在服務端執行
        if (level != null && !level.isClientSide && node != null) {
            ManaNetworkManager.getInstance().unregisterNode(node);
            node = null; // 清理 node
        }
    }


    public void transferManaToNetwork(int amount) {
        if (node != null) {
            node.receiveMana(amount);
        }
    }

    public void requestMana(Direction direction, int amount) {
        if (amount <= 0 || manaStored >= MAX_MANA) return;

        BlockEntity neighbor = this.level.getBlockEntity(this.worldPosition.relative(direction));
        if (neighbor != null) {
            neighbor.getCapability(ModCapabilities.MANA, direction.getOpposite()).ifPresent(neighborMana -> {
                int extracted = neighborMana.extractMana(amount, ManaAction.get(false));
                this.addMana(extracted);
            });
        }
    }

    public void overflowMana() {
        if (this.manaStored > MAX_MANA * 0.9) {
            for (Direction direction : Direction.values()) {
                requestMana(direction, 10); // 嘗試釋放多餘的魔力
            }
        }
    }

}
