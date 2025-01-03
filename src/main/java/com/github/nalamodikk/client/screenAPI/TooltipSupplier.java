package com.github.nalamodikk.client.screenAPI;

import net.minecraft.network.chat.Component;

import java.util.List;

@FunctionalInterface
public interface TooltipSupplier {
    List<Component> getTooltip();
}
