package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.Capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.Capability.ManaCapability;
import com.github.nalamodikk.common.Capability.ManaStorage;
import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.network.NetworkHandler;
import com.github.nalamodikk.common.network.TechWandModePacket;
import com.github.nalamodikk.common.screen.tool.UniversalConfigMenu;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber
public class BasicTechWandItem extends Item {

    public BasicTechWandItem(Properties properties) {
        super(properties);
    }

    // 定義科技魔杖的模式
    public enum TechWandMode {
        CONFIGURE_IO, // 合併輸入和輸出配置
        DIRECTION_CONFIG,
        ADD_MANA,
        ROTATE;

        public TechWandMode next() {
            TechWandMode[] modes = values();
            return modes[(this.ordinal() + 1) % modes.length];
        }

        public TechWandMode previous() {
            TechWandMode[] modes = values();
            return modes[(this.ordinal() - 1 + modes.length) % modes.length];
        }
    }

    public TechWandMode getMode(ItemStack stack) {
        return TechWandMode.values()[stack.getOrCreateTag().getInt("TechWandMode")];
    }

    public void setMode(ItemStack stack, TechWandMode mode) {
        stack.getOrCreateTag().putInt("TechWandMode", mode.ordinal());
    }

    // 伺服器端處理切換模式
    public void changeMode(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
            TechWandMode currentMode = getMode(stack);
            TechWandMode newMode = currentMode.next();
            setMode(stack, newMode);
            serverPlayer.displayClientMessage(Component.translatable("message.magical_industry.mode_changed",
                    Component.translatable("mode.magical_industry." + newMode.name().toLowerCase())), true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (player.isCrouching() && stack.getItem() instanceof BasicTechWandItem) {
            BlockPos pos = event.getPos();
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof IConfigurableBlock) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putInt("SelectedX", pos.getX());
                tag.putInt("SelectedY", pos.getY());
                tag.putInt("SelectedZ", pos.getZ());
                stack.setTag(tag);

                // 向玩家顯示一條消息，確認選擇了哪個方塊
                player.displayClientMessage(Component.translatable("message.magical_industry.block_selected", pos), true);
                event.setCanceled(true); // 防止進行默認左鍵行為，例如破壞方塊
            } else {
                player.displayClientMessage(Component.translatable("message.magical_industry.block_not_configurable"), true);
            }
        }
    }


    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!level.isClientSide && player != null && player.isCrouching()) {
            TechWandMode mode = getMode(stack);

            if (blockEntity != null) {
                // 如果是支持配置的機器，檢查 IConfigurableBlock 接口
                if (blockEntity instanceof IConfigurableBlock configurableBlock) {
                    Direction clickedFace = context.getClickedFace();

                    switch (mode) {
                        case DIRECTION_CONFIG:
                            // 獲取當前設置，切換方向配置
                            boolean isCurrentlyOutput = configurableBlock.isOutput(clickedFace);
                            configurableBlock.setDirectionConfig(clickedFace, !isCurrentlyOutput);
                            player.displayClientMessage(Component.translatable(
                                    "message.magical_industry.config_changed",
                                    clickedFace.getName(), !isCurrentlyOutput ? Component.translatable("mode.magical_industry.output") : Component.translatable("mode.magical_industry.input")), true);
                            return InteractionResult.SUCCESS;

                        case CONFIGURE_IO:
                            // 開啟配置界面
                            NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider(
                                    (id, playerInventory, playerEntity) -> new UniversalConfigMenu(id, playerInventory, blockEntity),
                                    Component.translatable("screen.magical_industry.configure_io")
                            ), pos);
                            return InteractionResult.SUCCESS;


                        case ADD_MANA:
                            // 與機器進行魔力交互
                            LazyOptional<IUnifiedManaHandler> manaCap = blockEntity.getCapability(ManaCapability.MANA);
                            if (manaCap.isPresent()) {
                                LazyOptional<IUnifiedManaHandler> wandManaCap = stack.getCapability(ManaCapability.MANA);
                                if (wandManaCap.isPresent()) {
                                    wandManaCap.ifPresent(wandMana -> {
                                        int manaToConsume = 10;
                                        if (wandMana.extractMana(manaToConsume, ManaAction.get(true)) >= manaToConsume) {
                                            wandMana.extractMana(manaToConsume, ManaAction.get(false));
                                            manaCap.ifPresent(mana -> mana.receiveMana(manaToConsume, ManaAction.get(false)));
                                            player.displayClientMessage(Component.translatable("message.magical_industry.add_mana_success"), true);
                                        } else {
                                            player.displayClientMessage(Component.translatable("message.magical_industry.not_enough_mana"), true);
                                        }
                                    });
                                    return InteractionResult.SUCCESS;
                                }
                            }
                            break;

                        case ROTATE:
                            // 旋轉機器方塊
                            BlockState state = level.getBlockState(pos);
                            if (state.hasProperty(BlockStateProperties.FACING)) {
                                Direction currentDirection = state.getValue(BlockStateProperties.FACING);
                                Direction newDirection = currentDirection.getClockWise();
                                BlockState newState = state.setValue(BlockStateProperties.FACING, newDirection);
                                level.setBlock(pos, newState, 3);
                                player.displayClientMessage(Component.translatable("message.magical_industry.block_rotated_facing"), true);
                                return InteractionResult.SUCCESS;
                            } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                                Direction currentDirection = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                                Direction newDirection = currentDirection.getClockWise();
                                BlockState newState = state.setValue(BlockStateProperties.HORIZONTAL_FACING, newDirection);
                                level.setBlock(pos, newState, 3);
                                player.displayClientMessage(Component.translatable("message.magical_industry.block_rotated_horizontal"), true);
                                return InteractionResult.SUCCESS;
                            } else {
                                player.displayClientMessage(Component.translatable("message.magical_industry.block_cannot_rotate"), true);
                            }
                            break;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && player.isCrouching()) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (heldItem.getItem() instanceof BasicTechWandItem) {
                BasicTechWandItem wand = (BasicTechWandItem) heldItem.getItem();
                float scrollDelta = (float) event.getScrollDelta();
                NetworkHandler.NETWORK_CHANNEL.sendToServer(new TechWandModePacket(scrollDelta > 0));
                event.setCanceled(true);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.magical_industry.mode", Component.translatable("mode.magical_industry." + getMode(stack).name().toLowerCase())));
    }

}
