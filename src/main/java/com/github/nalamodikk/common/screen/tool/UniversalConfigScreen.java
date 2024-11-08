package com.github.nalamodikk.common.screen.tool;

import com.github.nalamodikk.client.screenAPI.GenericButtonWithTooltip;
import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.network.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.NetworkHandler;
import com.github.nalamodikk.common.screen.tool.UniversalConfigMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
    private BlockEntity blockEntity;

    public UniversalConfigScreen(UniversalConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;

        // 嘗試從板手中獲取已選擇的方塊
        initializeBlockEntityFromWand(menu);
    }

    @Override
    protected void init() {
        super.init();

        // 初始化按鈕和界面設置
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        initDirectionButtons(centerX, centerY, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_TEXTURE_INPUT, BUTTON_TEXTURE_OUTPUT);
    }

    // 根據板手中的 NBT 設置要操作的方塊
    private void initializeBlockEntityFromWand(UniversalConfigMenu menu) {
        Player player = menu.getPlayer();
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (stack.getItem() instanceof BasicTechWandItem) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("SelectedX") && tag.contains("SelectedY") && tag.contains("SelectedZ")) {
                BlockPos selectedPos = new BlockPos(tag.getInt("SelectedX"), tag.getInt("SelectedY"), tag.getInt("SelectedZ"));
                Level level = player.level();
                BlockEntity blockEntity = level.getBlockEntity(selectedPos);

                if (blockEntity instanceof IConfigurableBlock) {
                    this.blockEntity = blockEntity;
                    // 初始化界面相關設置
                    for (Direction direction : Direction.values()) {
                        currentConfig.put(direction, ((IConfigurableBlock) blockEntity).isOutput(direction));
                    }
                } else {
                    player.displayClientMessage(Component.translatable("message.magical_industry.selected_block_unavailable"), true);
                }
            } else {
                player.displayClientMessage(Component.translatable("message.magical_industry.no_block_selected"), true);
            }
        }
    }

    private void initDirectionButtons(int baseX, int baseY, int width, int height, ResourceLocation textureInput, ResourceLocation textureOutput) {
        EnumMap<Direction, int[]> directionOffsets = new EnumMap<>(Direction.class);

        // 定義每個方向的偏移量
        directionOffsets.put(Direction.UP, new int[]{0, -60});
        directionOffsets.put(Direction.DOWN, new int[]{0, 60});
        directionOffsets.put(Direction.NORTH, new int[]{0, -30});
        directionOffsets.put(Direction.SOUTH, new int[]{0, 30});
        directionOffsets.put(Direction.WEST, new int[]{-60, 0});
        directionOffsets.put(Direction.EAST, new int[]{60, 0});

        // 為每個方向創建按鈕
        for (Direction direction : Direction.values()) {
            if (directionOffsets.containsKey(direction)) {
                int[] offset = directionOffsets.get(direction);
                int buttonX = baseX + offset[0];
                int buttonY = baseY + offset[1];

                ResourceLocation currentTexture = currentConfig.getOrDefault(direction, false) ? textureOutput : textureInput;

                // 初始化按鈕
                GenericButtonWithTooltip button = new GenericButtonWithTooltip(
                        buttonX, buttonY, width, height,
                        Component.translatable("direction.magical_industry." + direction.getName().toLowerCase()), // 使用本地化鍵名顯示方向名稱
                        currentTexture, 20, 20,
                        btn -> onDirectionConfigButtonClick(direction),
                        () -> {
                            // 提供工具提示
                            String configType = currentConfig.getOrDefault(direction, false) ? "output" : "input";
                            MutableComponent tooltipText = Component.translatable("screen.magical_industry.configure_side",
                                            Component.translatable("direction.magical_industry." + direction.getName().toLowerCase()))
                                    .append(" ")
                                    .append(Component.translatable("screen.magical_industry." + configType));
                            return Collections.singletonList(tooltipText);
                        }
                );
                // 添加按鈕到渲染列表中
                this.addRenderableWidget(button);
            }
        }
    }

    private void onDirectionConfigButtonClick(Direction direction) {
        if (blockEntity instanceof IConfigurableBlock configurableBlock) {
            // 切換方向配置並保存更改
            boolean newConfig = !configurableBlock.isOutput(direction);
            configurableBlock.setDirectionConfig(direction, newConfig);
            blockEntity.setChanged(); // 標記方塊狀態已更改

            // 同步方塊狀態到伺服器
            BlockPos pos = blockEntity.getBlockPos();
            NetworkHandler.NETWORK_CHANNEL.sendToServer(new ConfigDirectionUpdatePacket(pos, direction, newConfig));

            // 更新按鈕顯示
            currentConfig.put(direction, newConfig);
            updateButtonTooltip(direction);
            updateButtonTexture(getButtonByDirection(direction), direction, BUTTON_TEXTURE_INPUT, BUTTON_TEXTURE_OUTPUT);
        }
    }

    private void updateButtonTooltip(Direction direction) {
        GenericButtonWithTooltip button = getButtonByDirection(direction);
        if (button != null) {
            String configType = currentConfig.getOrDefault(direction, false) ? "output" : "input";
            Component tooltipText = Component.translatable("screen.magical_industry.configure_side",
                            Component.translatable("direction.magical_industry." + direction.getName().toLowerCase()))
                    .append(" ")
                    .append(Component.translatable("screen.magical_industry." + configType));
            button.setTooltip(Tooltip.create(tooltipText));
        }
    }

    private GenericButtonWithTooltip getButtonByDirection(Direction direction) {
        for (Object widget : this.renderables) {
            if (widget instanceof GenericButtonWithTooltip button && button.getMessage().getString().equals(direction.getName())) {
                return button;
            }
        }
        return null;
    }

    private void updateButtonTexture(GenericButtonWithTooltip button, Direction direction, ResourceLocation textureInput, ResourceLocation textureOutput) {
        if (button != null) {
            boolean isOutput = currentConfig.getOrDefault(direction, false);
            ResourceLocation newTexture = isOutput ? textureOutput : textureInput;
            button.setTexture(newTexture, 20, 20);
        }
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
}
