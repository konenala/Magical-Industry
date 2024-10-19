package com.github.nalamodikk.item;

import com.github.nalamodikk.block.ModBlocks;
import com.github.nalamodikk.MagicalIndustryMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB , MagicalIndustryMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAGICAL_INDUSTRY_ITEMS_TAB = CREATIVE_MODE_TABS.register("magical_industry_items_tab",
        () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MANA_DUST.get()))
                .title(Component.translatable("creativetab.magical_industry_items"))
                .displayItems((pParameters, pOutput) -> {

                    pOutput.accept(ModItems.MANA_DEBUG_TOOL.get());
                    pOutput.accept(ModItems.MANA_DUST.get());
                    pOutput.accept(ModItems.MANA_INGOT.get());

                    pOutput.accept(ModItems.CORRUPTED_MANA_DUST.get());



                })
                .build());



    public static final RegistryObject<CreativeModeTab> MAGICAL_INDUSTRY_BLOCKS_TAB = CREATIVE_MODE_TABS.register("magical_industry_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.MANA_BLOCK.get()))
                    .title(Component.translatable("creativetab.magical_industry_blocks"))
                    .displayItems((pParameters, pOutput) -> {

                        pOutput.accept(ModBlocks.MANA_BLOCK.get());
                        pOutput.accept(ModBlocks.MAGIC_ORE.get());
                        pOutput.accept(ModBlocks.DEEPSLATE_MAGIC_ORE.get());

                        pOutput.accept(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get());
                        pOutput.accept(ModBlocks.ADVANCED_MANA_CRAFTING_TABLE_BLOCK.get());


                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
