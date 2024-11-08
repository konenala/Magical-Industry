package com.github.nalamodikk.common.screen.tool;

import com.github.nalamodikk.client.screenAPI.GenericButtonWithTooltip;
import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class UniversalConfigScreen extends AbstractContainerScreen<UniversalConfigMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/universal_config.png");
    private static final ResourceLocation BUTTON_TEXTURE_INPUT = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/button_config_input.png");
    private static final ResourceLocation BUTTON_TEXTURE_OUTPUT = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/button_config_output.png");
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;

    private final EnumMap<Direction, Boolean> currentConfig = new EnumMap<>(Direction.class);
    private final BlockEntity blockEntity;

    public UniversalConfigScreen(UniversalConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;

        for (Direction direction : Direction.values()) {
            currentConfig.put(direction, false);
        }
    }

    @Override
    protected void init() {
        super.init();
        int baseX = (this.width - this.imageWidth) / 2 + this.imageWidth / 2 - BUTTON_WIDTH / 2;
        int baseY = (this.height - this.imageHeight) / 2 + this.imageHeight / 2 - BUTTON_HEIGHT / 2;

        updateCurrentConfigFromBlockEntity();
        initDirectionButtons(baseX, baseY);
        updateAllButtonTooltipsAndTextures();
    }

    private void initDirectionButtons(int baseX, int baseY) {
        EnumMap<Direction, int[]> directionOffsets = new EnumMap<>(Direction.class);

        directionOffsets.put(Direction.UP, new int[]{0, -60});
        directionOffsets.put(Direction.DOWN, new int[]{0, 60});
        directionOffsets.put(Direction.NORTH, new int[]{0, -30});
        directionOffsets.put(Direction.SOUTH, new int[]{0, 30});
        directionOffsets.put(Direction.WEST, new int[]{-60, 0});
        directionOffsets.put(Direction.EAST, new int[]{60, 0});

        for (Direction direction : Direction.values()) {
            if (directionOffsets.containsKey(direction)) {
                int[] offset = directionOffsets.get(direction);
                int buttonX = baseX + offset[0];
                int buttonY = baseY + offset[1];

                ResourceLocation currentTexture = currentConfig.getOrDefault(direction, false) ? BUTTON_TEXTURE_OUTPUT : BUTTON_TEXTURE_INPUT;

                GenericButtonWithTooltip button = new GenericButtonWithTooltip(
                        buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal(direction.getName()),
                        currentTexture, 20, 20,
                        btn -> onDirectionConfigButtonClick(direction),
                        () -> Collections.singletonList(getTooltipText(direction))
                );
                this.addRenderableWidget(button);
            }
        }
    }

    private void updateCurrentConfigFromBlockEntity() {
        if (blockEntity instanceof IConfigurableBlock configurableBlock) {
            for (Direction direction : Direction.values()) {
                currentConfig.put(direction, configurableBlock.isOutput(direction));
            }
        }
    }

    private void updateAllButtonTooltipsAndTextures() {
        for (Direction direction : Direction.values()) {
            GenericButtonWithTooltip button = getButtonByDirection(direction);
            if (button != null) {
                updateButtonTooltip(button, direction);
                updateButtonTexture(button, direction);
            }
        }
    }

    private GenericButtonWithTooltip getButtonByDirection(Direction direction) {
        return this.renderables.stream()
                .filter(widget -> widget instanceof GenericButtonWithTooltip button && button.getMessage().getString().equals(direction.getName()))
                .map(widget -> (GenericButtonWithTooltip) widget)
                .findFirst()
                .orElse(null);
    }

    private void onDirectionConfigButtonClick(Direction direction) {
        if (blockEntity instanceof IConfigurableBlock configurableBlock) {
            boolean newConfig = !currentConfig.getOrDefault(direction, false);
            currentConfig.put(direction, newConfig);

            // 更新本地按鈕狀態
            configurableBlock.setDirectionConfig(direction, newConfig);
            blockEntity.setChanged();

            // 發送封包來同步方向配置到伺服器
            NetworkHandler.NETWORK_CHANNEL.sendToServer(new ConfigDirectionUpdatePacket(blockEntity.getBlockPos(), direction, newConfig));

            // 顯示玩家通知
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(
                    "message.magical_industry.config_button_clicked",
                    direction.getName(), newConfig ? Component.translatable("mode.magical_industry.output") : Component.translatable("mode.magical_industry.input")), true);

            // 更新界面中的按鈕顯示
            updateAllButtonTooltipsAndTextures();
        }
    }


    private void updateButtonTooltip(GenericButtonWithTooltip button, Direction direction) {
        button.setTooltip(Tooltip.create(getTooltipText(direction)));
    }

    private void updateButtonTexture(GenericButtonWithTooltip button, Direction direction) {
        boolean isOutput = currentConfig.getOrDefault(direction, false);
        ResourceLocation newTexture = isOutput ? BUTTON_TEXTURE_OUTPUT : BUTTON_TEXTURE_INPUT;
        button.setTexture(newTexture, 20, 20);
    }

    private MutableComponent getTooltipText(Direction direction) {
        String configType = currentConfig.getOrDefault(direction, false) ? "output" : "input";
        return Component.translatable("screen.magical_industry.configure_side", direction.getName())
                .append(" ")
                .append(Component.translatable("screen.magical_industry." + configType));
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTicks) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752);
    }

    private boolean renderButtonTooltips(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
        for (Object widget : this.renderables) {
            if (widget instanceof GenericButtonWithTooltip button) {
                if (button.isMouseOver(mouseX, mouseY)) {
                    Tooltip tooltip = button.getTooltip();
                    if (tooltip != null) {
                        List<FormattedCharSequence> formattedComponents = tooltip.toCharSequence(Minecraft.getInstance());
                        if (!formattedComponents.isEmpty()) {
                            pGuiGraphics.renderTooltip(Minecraft.getInstance().font, formattedComponents, mouseX, mouseY);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
