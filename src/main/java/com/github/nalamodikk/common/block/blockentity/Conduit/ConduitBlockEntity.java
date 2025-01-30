package com.github.nalamodikk.common.block.blockentity.Conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

public abstract class ConduitBlockEntity extends BlockEntity {

    private final EnumMap<Direction, Boolean> connections = new EnumMap<>(Direction.class);

    public ConduitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // 初始化連接狀態
        for (Direction direction : Direction.values()) {
            connections.put(direction, false);
        }
    }

    public Map<Direction, Boolean> getConnections() {
        return connections;
    }

    public void setConnection(Direction direction, boolean connected) {
        connections.put(direction, connected);
        setChanged();
    }

    public boolean isConnected(Direction direction) {
        return connections.getOrDefault(direction, false);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // 清理資源（如果有需要）
    }
}
