package com.github.nalamodikk.screen.mana_crafting_table;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.block.entity.mana_crafting_table.AdvancedManaCraftingTableBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AdvancedManaCraftingTableScreen extends AbstractContainerScreen<AdvancedManaCraftingTableMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/advanced_mana_crafting_table_gui.png");
    private static final ResourceLocation MANA_BAR_FULL = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_full.png");

    public AdvancedManaCraftingTableScreen(AdvancedManaCraftingTableMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        this.imageWidth = 255;  // GUI 界面的寬度
        this.imageHeight = 202;  // GUI 界面的高度
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // 渲染背景纹理
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // 动态渲染魔力条
        renderManaBar(guiGraphics);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 渲染屏幕上的文本标签
        guiGraphics.drawString(this.font, this.title.getString(), this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle.getString(), 8, this.imageHeight - 94, 4210752, false);
    }

    private void renderManaBar(@NotNull GuiGraphics guiGraphics) {
        // 获取魔力条的位置
        int manaBarX = this.leftPos + 10;
        int manaBarY = this.topPos + 15;
        int manaBarWidth = 11;
        int manaBarHeight = 49;

        // 使用 menu 中的 getManaStored() 方法获取当前的魔力存储量
        int manaStored = this.menu.getManaStored();
        int maxMana = AdvancedManaCraftingTableBlockEntity.MAX_MANA;
        float manaPercentage = (float) manaStored / maxMana;
        int manaHeight = Math.round(manaPercentage * manaBarHeight);

        // 渲染满的魔力条部分
        if (manaHeight > 0) {
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(MANA_BAR_FULL, manaBarX, manaBarY + (manaBarHeight - manaHeight), 0, 0, manaBarWidth, manaHeight);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderManaTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderManaTooltip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int manaBarX = this.leftPos + 10;
        int manaBarY = this.topPos + 15;
        int manaBarWidth = 11;
        int manaBarHeight = 49;

        if (mouseX >= manaBarX && mouseX <= manaBarX + manaBarWidth &&
                mouseY >= manaBarY && mouseY <= manaBarY + manaBarHeight) {
            int manaStored = this.menu.getManaStored();
            int maxMana = AdvancedManaCraftingTableBlockEntity.MAX_MANA;

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("tooltip.magical_industry.mana_stored", manaStored, maxMana));

            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}
