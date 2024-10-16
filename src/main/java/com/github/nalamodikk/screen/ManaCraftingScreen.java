package com.github.nalamodikk.screen;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.block.entity.ManaCraftingTableBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ManaCraftingScreen extends AbstractContainerScreen<ManaCraftingMenu> {
    private static final ResourceLocation MANA_BAR_FULL = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_full.png");
    private static final ResourceLocation MANA_BAR_EMPTY = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_empty.png");
    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_crafting_table_gui.png");

    public ManaCraftingScreen(ManaCraftingMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        this.imageWidth = 176;  // GUI 界面的宽度
        this.imageHeight = 166;  // GUI 界面的高度
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // 渲染背景纹理
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // 获取魔力条的位置
        int manaBarX = this.leftPos + 10;
        int manaBarY = this.topPos + 15;

        // 渲染空的魔力条
        RenderSystem.setShaderTexture(0, MANA_BAR_EMPTY);
        guiGraphics.blit(MANA_BAR_EMPTY, manaBarX, manaBarY, 0, 0, 10, 50);

        // 使用 menu 中的 getManaStored() 方法获取当前的魔力存储量
        int manaStored = this.menu.getManaStored();
        System.out.println("Debug: Current Mana Stored (GUI): " + manaStored); // 调试输出当前魔力值
        int maxMana = ManaCraftingTableBlockEntity.MAX_MANA;
        int manaHeight = (int) ((float) manaStored / maxMana * 50);

        // 渲染满的魔力条
        if (manaHeight > 0) {
            RenderSystem.setShaderTexture(0, MANA_BAR_FULL);
            guiGraphics.blit(MANA_BAR_FULL, manaBarX, manaBarY + (50 - manaHeight), 0, 0, 10, manaHeight);
        }
    }


    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 渲染屏幕上的文本标签
        guiGraphics.drawString(this.font, this.title.getString(), this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle.getString(), 8, this.imageHeight - 94, 4210752, false);
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
        int manaBarX = this.leftPos + 10; // 设置魔力条的 X 位置
        int manaBarY = this.topPos + 15;  // 设置魔力条的 Y 位置
        int manaBarWidth = 10;            // 魔力条的宽度
        int manaBarHeight = 50;           // 魔力条的高度

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