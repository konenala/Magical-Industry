package com.github.nalamodikk.common.block;

import com.github.nalamodikk.common.block.custom.ManaGeneratorBlock;
import com.github.nalamodikk.common.block.custom.mana_crafting_table.ManaCraftingTableBlock;
import com.github.nalamodikk.common.item.ModItems;
import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MagicalIndustryMod.MOD_ID);

    // 註冊 Mana Block
    public static final RegistryObject<Block> MANA_BLOCK = registerBlock("mana_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<Block> MANA_CRAFTING_TABLE_BLOCK = registerBlock("mana_crafting_table",
            () -> new ManaCraftingTableBlock(BlockBehaviour.Properties.copy(Blocks.BIRCH_WOOD)));

    public static final RegistryObject<Block> MANA_GENERATOR = BLOCKS.register("mana_generator",
            () -> new ManaGeneratorBlock(BlockBehaviour.Properties.copy(Blocks.STONE).noOcclusion()));



    // 新增魔法礦物的註冊
    public static final RegistryObject<Block> MAGIC_ORE = registerBlock("magic_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(2f).requiresCorrectToolForDrops(), UniformInt.of(3, 6)));

    public static final RegistryObject<Block> DEEPSLATE_MAGIC_ORE = registerBlock("deepslate_magic_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE)
                    .strength(4f).requiresCorrectToolForDrops(), UniformInt.of(3, 8)));





    // 通用的註冊方法
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
