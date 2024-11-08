package com.github.nalamodikk.common.block.entity.basic;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;

public abstract class ConfigurableBlockBase extends BlockEntity implements IConfigurableBlock {
    protected final EnumMap<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);

    public ConfigurableBlockBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setDirectionConfig(Direction direction, boolean isOutput) {
        directionConfig.put(direction, isOutput);
        setChanged();  // 標記為更新
    }

    @Override
    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }

    // 通用的保存和加載方法
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("DirectionConfig", Tag.TAG_COMPOUND)) {
            CompoundTag directionTag = tag.getCompound("DirectionConfig");
            for (Direction direction : Direction.values()) {
                this.directionConfig.put(direction, directionTag.getBoolean(direction.getName()));
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag directionTag = new CompoundTag();
        for (Direction direction : Direction.values()) {
            directionTag.putBoolean(direction.getName(), this.directionConfig.getOrDefault(direction, false));
        }
        tag.put("DirectionConfig", directionTag);
    }
}
