package com.github.nalamodikk.common;

import com.github.nalamodikk.client.renderer.ManaGeneratorRenderer;
import com.github.nalamodikk.common.block.ModBlocks;
import com.github.nalamodikk.common.block.entity.mana_crafting.ManaCraftingTableBlockEntity;
import com.github.nalamodikk.common.block.entity.ModBlockEntities;
import com.github.nalamodikk.common.Capability.ModCapabilities;  // 新增的导入
import com.github.nalamodikk.common.item.ModCreativeModTabs;
import com.github.nalamodikk.common.item.ModItems;
import com.github.nalamodikk.common.network.NetworkHandler;
import com.github.nalamodikk.common.recipe.ModRecipes;
import com.github.nalamodikk.common.register.ModMenuScreens;
import com.github.nalamodikk.common.register.ModRenderers;
import com.github.nalamodikk.common.screen.manacrafting.ManaCraftingScreen;
import com.github.nalamodikk.common.screen.ModMenusTypes;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent; // 用于附加 Capability
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MagicalIndustryMod.MOD_ID)
public class MagicalIndustryMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "magical_industry";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();


    public MagicalIndustryMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        GeckoLib.initialize();
        // 注册创造模式标签
        ModCreativeModTabs.register(modEventBus);

        // 注册物品和方块
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        modEventBus.register(ModCapabilities.class);

        // 注册菜单类型和方块实体和配方
        ModMenusTypes.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModRecipes.register(modEventBus);

        // 注册模组的生命周期事件
        modEventBus.addListener(this::commonSetup);

        // 注册创造模式标签的内容
        modEventBus.addListener(this::addCreative);

        // 注册 MinecraftForge 的事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 通用设置
        NetworkHandler.init(event);

    }


    // 添加物品到创造模式标签
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 在这里添加物品到相应的创造模式标签
    }

    // 服务器启动事件
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    // 附加 Capability 给 ManaCraftingTableBlockEntity
    // 附加 Capability 给 ManaCraftingTableBlockEntity
    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<?> event) {
        if (event.getObject() instanceof ManaCraftingTableBlockEntity blockEntity) {
            event.addCapability(new ResourceLocation(MOD_ID, "mana"), new ManaCraftingTableBlockEntity.Provider(blockEntity));
        }
    }


    // 客户端事件订阅器
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // 客户端设置
            ModMenuScreens.registerScreens();
            ModRenderers.registerBlockEntityRenderers();
         //   BlockEntityRenderers.register(ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorRenderer::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
           // event.registerBlockEntityRenderer(ModBlockEntities.MANA_GENERATOR_BE.get(), rendererContext -> new ManaGeneratorRenderer());

        }

    }




}