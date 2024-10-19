package com.github.nalamodikk.block.entity;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.block.ModBlocks;
import com.github.nalamodikk.block.entity.mana_crafting_table.BaseManaCraftingTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MagicalIndustryMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<BaseManaCraftingTableBlockEntity>> MANA_CRAFTING_TABLE_BLOCK_BE =
            BLOCK_ENTITIES.register("mana_crafting_table_be", () ->
                    BlockEntityType.Builder.of(BaseManaCraftingTableBlockEntity::new,
                            ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<BaseManaCraftingTableBlockEntity>> ADVANCED_MANA_CRAFTING_TABLE_BLOCK_BE =
            BLOCK_ENTITIES.register("advanced_mana_crafting_table_block_be", () ->
                    BlockEntityType.Builder.of(com.github.nalamodikk.block.entity.mana_crafting_table.BaseManaCraftingTableBlockEntity::new,
                            ModBlocks.ADVANCED_MANA_CRAFTING_TABLE_BLOCK.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}