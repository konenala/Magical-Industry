package com.github.nalamodikk.common.worldgen;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.ModBlocks;
//eimport com.github.nalamodikk.common.worldgen.tree.custom.PineFoliagePlacer;
//import com.github.nalamodikk.common.worldgen.tree.custom.PineTrunkPlacer;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_MAGIC_ORE_KEY = registerKey("magic_ore");
   /* public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_MAGIC_ORE_KEY = registerKey("nether_sapphire_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> END_SAPPHIRE_ORE_KEY = registerKey("end_sapphire_ore");
    */

    public static final ResourceKey<ConfiguredFeature<?, ?>> PINE_KEY = registerKey("pine");

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceable = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherrackReplacables = new BlockMatchTest(Blocks.NETHERRACK);
        RuleTest endReplaceables = new BlockMatchTest(Blocks.END_STONE);

        List<OreConfiguration.TargetBlockState> overworldSapphireOres = List.of(OreConfiguration.target(stoneReplaceable,
                ModBlocks.MAGIC_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_MAGIC_ORE.get().defaultBlockState()));

        register(context, OVERWORLD_MAGIC_ORE_KEY, Feature.ORE, new OreConfiguration(overworldSapphireOres, 9));
       /* register(context, NETHER_SAPPHIRE_ORE_KEY, Feature.ORE, new OreConfiguration(netherrackReplacables,
                ModBlocks.NETHER_SAPPHIRE_ORE.get().defaultBlockState(), 9));
        register(context, END_SAPPHIRE_ORE_KEY, Feature.ORE, new OreConfiguration(endReplaceables,
                ModBlocks.END_STONE_SAPPHIRE_ORE.get().defaultBlockState(), 9));

        */

       /* register(context, PINE_KEY, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.PINE_LOG.get()),
                new PineTrunkPlacer(5, 4, 3),

                BlockStateProvider.simple(ModBlocks.PINE_LEAVES.get()),
                new PineFoliagePlacer(ConstantInt.of(3), ConstantInt.of(2), 3),


                new TwoLayersFeatureSize(1, 0, 2)).build());
                    */

    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(MagicalIndustryMod.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}