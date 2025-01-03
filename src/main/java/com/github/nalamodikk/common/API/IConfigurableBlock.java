package com.github.nalamodikk.common.API;

import net.minecraft.core.Direction;

public interface IConfigurableBlock {
    void setDirectionConfig(Direction direction, boolean isOutput);
    boolean isOutput(Direction direction);
}
