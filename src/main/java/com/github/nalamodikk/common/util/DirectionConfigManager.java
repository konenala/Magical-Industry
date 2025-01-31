package com.github.nalamodikk.common.util;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

public class DirectionConfigManager {
    private final Map<Direction, Boolean> directionConfig = new EnumMap<>(Direction.class);

    public void setDirectionConfig(Direction direction, boolean isOutput) {
        directionConfig.put(direction, isOutput);
    }

    public boolean isOutput(Direction direction) {
        return directionConfig.getOrDefault(direction, false);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<Direction, Boolean> entry : directionConfig.entrySet()) {
            tag.putBoolean(entry.getKey().getName(), entry.getValue());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        directionConfig.clear();
        for (Direction direction : Direction.values()) {
            directionConfig.put(direction, tag.getBoolean(direction.getName()));
        }
    }
}
