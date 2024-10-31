package com.github.nalamodikk.common.screen.ManaGenerator;

import com.github.nalamodikk.client.screenAPI.UniversalTexturedButton;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.NetworkHandler;
import com.github.nalamodikk.common.network.ToggleModePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.BlockPos;

public class ManaGeneratorScreen extends AbstractContainerScreen<ManaGeneratorMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_generator_gui.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_generator_button_texture.png");
    private static final ResourceLocation MANA_BAR_FULL = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_full.png");
    private static final ResourceLocation ENERGY_BAR_FULL = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/energy_bar_full.png");
    private static final ResourceLocation FUEL_TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/fuel_bar.png");

    private UniversalTexturedButton toggleModeButton;

    public ManaGeneratorScreen(ManaGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 創建並添加自定義紋理按鈕
        toggleModeButton = new UniversalTexturedButton(
                x + 130, y + 25, 20, 20,
                Component.empty(),
                BUTTON_TEXTURE, 20, 20,
                btn -> {
                    // 發送封包到伺服器
                    BlockPos blockPos = this.menu.getBlockEntityPos();
                    NetworkHandler.NETWORK_CHANNEL.sendToServer(new ToggleModePacket(blockPos));
//                    MagicalIndustryMod.LOGGER.info("ToggleModePacket sent for block at: " + blockPos);
                }
        );


        // 將按鈕添加到屏幕中
        this.addRenderableWidget(toggleModeButton);
        toggleModeButton.setTooltip(Tooltip.create(Component.translatable("screen.magical_industry.toggle_mode"))); // 設置工具提示，當滑鼠懸停時顯示
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 渲染魔力條
        int manaBarHeight = 47;
        int manaBarWidth = 7;
        int mana = this.menu.getManaStored();
        int maxMana = this.menu.getMaxMana();
        if (maxMana > 0 && mana > 0) {
            int renderHeight = (int) (((float) mana / maxMana) * manaBarHeight);
            RenderSystem.setShaderTexture(0, MANA_BAR_FULL);
            pGuiGraphics.blit(MANA_BAR_FULL, this.leftPos + 11, this.topPos + 19 + (manaBarHeight - renderHeight), 49, 11, manaBarWidth, renderHeight);
        }

        // 渲染能量條（在右側）
        int energyBarHeight = 47;
        int energyBarWidth = 7;
        int energy = this.menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergy();
        if (maxEnergy > 0 && energy > 0) {
            int renderHeight = (int) (((float) energy / maxEnergy) * energyBarHeight);
            RenderSystem.setShaderTexture(0, ENERGY_BAR_FULL);
            pGuiGraphics.blit(ENERGY_BAR_FULL, this.leftPos + 158, this.topPos + 19 + (energyBarHeight - renderHeight), 49, 11, energyBarWidth, renderHeight);
        }
    }


    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        super.renderLabels(pGuiGraphics, pMouseX, pMouseY);
        String modeText = this.menu.getCurrentMode() == 1
                ? Component.translatable("mode.magical_industry.energy").getString()
                : Component.translatable("mode.magical_industry.mana").getString();
        Component currentMode = Component.translatable("screen.magical_industry.current_mode", modeText);
        int x = (this.imageWidth - this.font.width(currentMode)) / 2;
        int y = 5;
        pGuiGraphics.drawString(this.font, currentMode, x, y, 4210752);

        // 渲染燃料進度條
        int burnTime = this.menu.getBurnTime();
        int currentBurnTime = this.menu.getCurrentBurnTime();
        if (currentBurnTime > 0 && burnTime > 0) {
            int fuelHeight = (int) ((float) burnTime / currentBurnTime * 13);
            RenderSystem.setShaderTexture(0, FUEL_TEXTURE);
            pGuiGraphics.blit(FUEL_TEXTURE, 56, 36 + 12 - fuelHeight, 0, 12 - fuelHeight, 14, fuelHeight);
        }
    }





    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTicks) {
        this.renderBackground(pGuiGraphics); // 確保背景被正確渲染
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
}
