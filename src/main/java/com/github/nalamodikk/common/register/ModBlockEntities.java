package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.NeoMagnaMod;
import com.github.nalamodikk.common.block.blockentity.Conduit.ManaConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.ManaGenerator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, NeoMagnaMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ManaCraftingTableBlockEntity>> MANA_CRAFTING_TABLE_BLOCK_BE =
            BLOCK_ENTITIES.register("mana_crafting_table_be", () ->
                    BlockEntityType.Builder.of(ManaCraftingTableBlockEntity::new,
                            ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<ManaGeneratorBlockEntity>> MANA_GENERATOR_BE = BLOCK_ENTITIES.register("mana_generator",
            () -> BlockEntityType.Builder.of(ManaGeneratorBlockEntity::new, ModBlocks.MANA_GENERATOR.get())
                    .build(null));



    public static final RegistryObject<BlockEntityType<ManaConduitBlockEntity>> MANA_CONDUIT_BE =
            BLOCK_ENTITIES.register("mana_conduit_be",
                    () -> BlockEntityType.Builder.of(
                            ManaConduitBlockEntity::new, // BlockEntity 的構造函數
                            ModBlocks.MANA_CONDUIT.get() // 支援的方塊
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}