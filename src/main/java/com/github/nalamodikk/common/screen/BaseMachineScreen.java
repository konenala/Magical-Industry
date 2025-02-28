package com.github.nalamodikk.client.screen;

import com.github.nalamodikk.common.screen.BaseMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;

public abstract class BaseMachineScreen<T extends BaseMachineMenu> extends AbstractContainerScreen<T> {
    protected final ResourceLocation texture;

    public BaseMachineScreen(T menu, Inventory playerInventory, Component title, ResourceLocation texture) {
        super(menu, playerInventory, title);
        this.texture = texture;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        gui.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
        renderTooltip(gui, mouseX, mouseY);
    }

    protected void drawProgressBar(GuiGraphics gui, int x, int y, int progress) {
        int progressWidth = (int) (progress / 100.0 * 24);
        gui.blit(texture, leftPos + x, topPos + y, 176, 14, progressWidth, 16);
    }

    protected void drawEnergyBar(GuiGraphics gui, int x, int y, int energy) {
        int energyHeight = (int) (energy / 100.0 * 50);
        gui.blit(texture, leftPos + x, topPos + y + (50 - energyHeight), 176, 31, 16, energyHeight);
    }
}
