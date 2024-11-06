package com.github.nalamodikk.common.screen.tool;

import com.github.nalamodikk.client.screenAPI.UniversalTexturedButton;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.NetworkHandler;
import com.github.nalamodikk.common.screen.tool.UniversalConfigMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumMap;

public class UniversalConfigScreen extends AbstractContainerScreen<UniversalConfigMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/universal_config.png");
    private final EnumMap<Direction, Boolean> currentConfig = new EnumMap<>(Direction.class);
    private final BlockEntity blockEntity;

    public UniversalConfigScreen(UniversalConfigMenu menu, Inventory playerInventory, Component title, BlockEntity blockEntity) {
        super(menu, playerInventory, title);
        this.blockEntity = blockEntity;

        this.imageWidth = 176;
        this.imageHeight = 166;

        // 初始化 currentConfig，設置每個方向的默認值
        for (Direction direction : Direction.values()) {
            currentConfig.put(direction, false); // 默認所有方向都不是輸出
        }
    }

    @Override
    protected void init() {
        super.init();

        ResourceLocation buttonTexture = new ResourceLocation("magical_industry", "textures/gui/button_config.png");
        int buttonWidth = 20;
        int buttonHeight = 20;

        // 創建四個按鈕對應不同的方向
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = this.height / 2 - 50;

        for (Direction direction : Direction.values()) {
            int currentX = buttonX + direction.getStepX() * 30; // 根據方向調整按鈕位置
            int currentY = buttonY + direction.getStepY() * 30;

            UniversalTexturedButton configButton = new UniversalTexturedButton(
                    currentX,
                    currentY,
                    buttonWidth,
                    buttonHeight,
                    Component.literal(direction.getName()), // 顯示方向名稱
                    buttonTexture,
                    256,
                    256,
                    button -> {
                        // 當按下配置按鈕時執行的操作
                        this.onDirectionConfigButtonClick(direction);
                    }
            );

            this.addRenderableWidget(configButton);
        }
    }

    // 當點擊配置按鈕時執行的操作
    private void onDirectionConfigButtonClick(Direction direction) {
        // 更新本地狀態，例如切換該方向的輸入/輸出模式
        this.currentConfig.put(direction, !this.currentConfig.getOrDefault(direction, false));

        // 向服務端發送封包來同步更改
        NetworkHandler.NETWORK_CHANNEL.sendToServer(new ConfigDirectionUpdatePacket(this.blockEntity.getBlockPos(), direction, this.currentConfig.get(direction)));
        Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.magical_industry.config_button_clicked"), true);

    }



    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float partialTicks, int mouseX, int mouseY) {

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
        super.renderLabels(pGuiGraphics, mouseX, mouseY);


    }


    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTicks) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);



        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }


}
