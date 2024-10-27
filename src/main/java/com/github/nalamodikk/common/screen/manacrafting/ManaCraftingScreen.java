package com.github.nalamodikk.common.screen.manacrafting;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.mana_crafting.ManaCraftingTableBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ManaCraftingScreen extends AbstractContainerScreen<ManaCraftingMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_crafting_table_gui.png");
    private static final ResourceLocation MANA_BAR_FULL = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_full.png");
    private static final Logger LOGGER = LogManager.getLogger();

    public ManaCraftingScreen(ManaCraftingMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        this.imageWidth = 176;  // GUI 界面的宽度
        this.imageHeight = 166;  // GUI 界面的高度
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // 渲染背景纹理

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);


        // 动态渲染魔力条
        renderManaBar(guiGraphics, partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 渲染屏幕上的文本标签
        guiGraphics.drawString(this.font, this.title.getString(), this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle.getString(), 8, this.imageHeight - 94, 4210752, false);
    }

    private void renderManaBar(GuiGraphics guiGraphics, float partialTicks) {
        // 獲取魔力條的位置
        int manaBarX = this.leftPos + 11;
        int manaBarY = this.topPos + 19;
        int manaBarWidth = 7;
        int manaBarHeight = 47;

        // 使用 menu 中的 getManaStored() 方法獲取當前的魔力存儲量
        int manaStored = this.menu.getManaStored();
        int maxMana = ManaCraftingTableBlockEntity.MAX_MANA;
        float manaPercentage = (float) manaStored / maxMana;
        int manaHeight = Math.round(manaPercentage * manaBarHeight);

        // 渲染滿的魔力條部分，使用獨立的滿魔力條圖片
        if (manaHeight > 0) {
            // 禁用混合模式來避免透明度問題
            RenderSystem.disableBlend();

            // 設置紋理顏色為不透明
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // 渲染滿魔力條
            guiGraphics.blit(MANA_BAR_FULL, manaBarX, manaBarY + (manaBarHeight - manaHeight), 49, 11, manaBarWidth, manaHeight);
        }
    }



    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染背景和前景
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        // 渲染工具提示
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // 添加鼠标悬停提示
        renderManaTooltip(guiGraphics, mouseX, mouseY);
    }

    // 添加一个方法来处理魔力条的鼠标悬停提示
    private void renderManaTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 魔力条的位置和大小
        int manaBarX = this.leftPos + 11;
        int manaBarY = this.topPos + 19;
        int manaBarWidth = 7;
        int manaBarHeight = 47;

        // 检查鼠标是否在魔力条的范围内
        if (mouseX >= manaBarX && mouseX <= manaBarX + manaBarWidth &&
                mouseY >= manaBarY && mouseY <= manaBarY + manaBarHeight) {

            // 使用 menu 中的 getManaStored() 方法获取当前的魔力值
            int manaStored = this.menu.getManaStored();
            int maxMana = ManaCraftingTableBlockEntity.MAX_MANA;

            // 创建要显示的工具提示内容
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("tooltip.magical_industry.mana_stored", manaStored, maxMana));

            // 显示工具提示
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}
