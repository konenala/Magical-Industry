package com.github.nalamodikk.common.screen.ManaGenerator;

import com.github.nalamodikk.client.screenAPI.UniversalTexturedButton;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.ManaGenerator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.network.NetworkHandler;
import com.github.nalamodikk.common.network.ToggleModePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
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
    private static final int MANA_BAR_HEIGHT = 47;
    private static final int MANA_BAR_WIDTH = 7;
    private static final int ENERGY_BAR_HEIGHT = 47;
    private static final int ENERGY_BAR_WIDTH = 7;
    private static final int TOGGLE_BUTTON_X_OFFSET = 130;
    private static final int TOGGLE_BUTTON_Y_OFFSET = 25;

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
                x + TOGGLE_BUTTON_X_OFFSET, y + TOGGLE_BUTTON_Y_OFFSET, 20, 20,
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
//
//        // 渲染魔力條
//        int manaBarHeight = 47;
//        int manaBarWidth = 7;
//        int mana = this.menu.getManaStored();
//        int maxMana = this.menu.getMaxMana();
//        if (maxMana > 0 && mana > 0) {
//            int renderHeight = (int) (((float) mana / maxMana) * manaBarHeight);
//            RenderSystem.setShaderTexture(0, MANA_BAR_FULL);
//            pGuiGraphics.blit(MANA_BAR_FULL, this.leftPos + 11, this.topPos + 19 + (manaBarHeight - renderHeight), 49, 11, manaBarWidth, renderHeight);
//        }

        drawManaBar(pGuiGraphics, 11, 19); // 魔力條的位置偏移（可以根據需要調整）

        drawEnergyBar(pGuiGraphics, 156, 19); // 這裡的 xOffset 和 yOffset 是相對於 GUI 左上角的位置偏移

//        // 渲染能量條（在右側）
//        int energyBarHeight = 47;
//        int energyBarWidth = 8;
//        int energy = this.menu.getEnergyStored(); // 从 containerData 中获取能量值
//        int maxEnergy = this.menu.getMaxEnergy();
//        if (maxEnergy > 0 && energy > 0) {
//            int renderHeight = (int) (((float) energy / maxEnergy) * ENERGY_BAR_HEIGHT);
//            RenderSystem.setShaderTexture(0, ENERGY_BAR_FULL);
//            pGuiGraphics.blit(ENERGY_BAR_FULL, this.leftPos + 156, this.topPos + 19 + (energyBarHeight - renderHeight), 49, 11, energyBarWidth, renderHeight);
//        }

    }

    private void drawEnergyBar(GuiGraphics pGuiGraphics, int xOffset, int yOffset) {
        int energyBarHeight = 47;
        int energyBarWidth = 8;
        int energy = menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergy();

        if (maxEnergy > 0 && energy > 0) {
            int renderHeight = (int) (((float) energy / maxEnergy) * energyBarHeight); // 計算應該渲染的高度
            RenderSystem.setShaderTexture(0, ENERGY_BAR_FULL); // 設置能量條的紋理
            pGuiGraphics.blit(ENERGY_BAR_FULL, this.leftPos + xOffset, this.topPos + yOffset + (energyBarHeight - renderHeight),
                    49, 11, energyBarWidth, renderHeight);
        }
    }

    private void drawManaBar(GuiGraphics pGuiGraphics, int xOffset, int yOffset) {
        int manaBarHeight = 47;
        int manaBarWidth = 8;
        int mana = this.menu.getManaStored(); // 從 containerData 中獲取魔力值
        int maxMana = this.menu.getMaxMana();

        if (maxMana > 0 && mana > 0) {
            int renderHeight = (int) (((float) mana / maxMana) * manaBarHeight); // 計算應該渲染的高度
            RenderSystem.setShaderTexture(0, MANA_BAR_FULL); // 設置魔力條的紋理
            pGuiGraphics.blit(MANA_BAR_FULL, this.leftPos + xOffset, this.topPos + yOffset + (manaBarHeight - renderHeight),
                    49, 11, manaBarWidth, renderHeight);
        }
    }


    private int getFuelProgressHeight() {
        int burnTime = this.menu.getBurnTime();
        int currentBurnTime = this.menu.getCurrentBurnTime();
        return currentBurnTime > 0 ? (int) ((float) burnTime / currentBurnTime * 13) : 0;
    }

    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        super.renderLabels(pGuiGraphics, pMouseX, pMouseY);
        int fuelHeight = getFuelProgressHeight();
        if (fuelHeight > 0) {
            RenderSystem.setShaderTexture(0, FUEL_TEXTURE);
            pGuiGraphics.blit(FUEL_TEXTURE, 56, 36 + 12 - fuelHeight, 36, 36 - fuelHeight, 14, fuelHeight);
        }


        String modeText = this.menu.getCurrentMode() == 1
                ? Component.translatable("mode.magical_industry.energy").getString()
                : Component.translatable("mode.magical_industry.mana").getString();
        Component currentMode = Component.translatable("screen.magical_industry.current_mode", modeText);


        // 設置文字的初始渲染位置（在縮放之前）
        float originalX = (this.imageWidth - this.font.width(currentMode)) / 2f;
        float originalY = 15f; // 假設你想讓文本在 Y 軸上距離 10 像素的位置顯示


        // 從 pGuiGraphics 中獲取 PoseStack 以進行縮放操作
        PoseStack poseStack = pGuiGraphics.pose();

        // 設置縮放比例
        float scale = 0.85f;

        // 保存當前渲染狀態
        poseStack.pushPose();

        // 縮放文本
        poseStack.scale(scale, scale, scale);

        // 因為文字縮放了，所以位置也需要縮放來保證顯示正常
        float scaledX = originalX / scale;
        float scaledY = originalY / scale;


        // 渲染文字
        pGuiGraphics.drawString(this.font, currentMode, (int) scaledX, (int) scaledY, 4210752);

        // 恢復渲染狀態
        poseStack.popPose();

    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTicks) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);

        if (isHoveringManaBar(pMouseX, pMouseY)) {
            pGuiGraphics.renderTooltip(this.font, Component.translatable("tooltip.mana", this.menu.getManaStored(), this.menu.getMaxMana()), pMouseX, pMouseY);
        }

        if (isHoveringEnergyBar(pMouseX, pMouseY)) {
            pGuiGraphics.renderTooltip(this.font, Component.translatable("tooltip.energy", this.menu.getEnergyStored(), this.menu.getMaxEnergy()), pMouseX, pMouseY);
        }

        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    private boolean isHoveringManaBar(int mouseX, int mouseY) {
        int manaBarX = this.leftPos + 11;
        int manaBarY = this.topPos + 19;
        return mouseX >= manaBarX && mouseX <= manaBarX + MANA_BAR_WIDTH && mouseY >= manaBarY && mouseY <= manaBarY + MANA_BAR_HEIGHT;
    }

    private boolean isHoveringEnergyBar(int mouseX, int mouseY) {
        int energyBarX = this.leftPos + 157;
        int energyBarY = this.topPos + 19;
        return mouseX >= energyBarX && mouseX <= energyBarX + ENERGY_BAR_WIDTH && mouseY >= energyBarY && mouseY <= energyBarY + ENERGY_BAR_HEIGHT;
    }

}
