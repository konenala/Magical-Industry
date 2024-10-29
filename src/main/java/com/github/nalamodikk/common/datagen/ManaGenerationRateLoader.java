package com.github.nalamodikk.common.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = "magical_industry", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ManaGenerationRateLoader implements DataProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Map<ResourceLocation, Integer> MANA_GENERATION_RATES = new HashMap<>();
    public static final Map<ResourceLocation, Integer> ENERGY_GENERATION_RATES = new HashMap<>();
    private final PackOutput packOutput;

    // 加載魔力和能量生成速率
    public static void loadManaAndEnergyGenerationRates() {
        try (InputStreamReader reader = new InputStreamReader(
                ManaGenerationRateLoader.class.getResourceAsStream("/data/magical_industry/mana_energy_generation_rates.json"))) {
            Type type = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();
            Map<String, Map<String, Integer>> rates = GSON.fromJson(reader, type);

            // 加載魔力生成速率
            rates.getOrDefault("mana_rates", new HashMap<>()).forEach((key, value) ->
                    MANA_GENERATION_RATES.put(new ResourceLocation(key), value)
            );

            // 加載能量生成速率
            rates.getOrDefault("energy_rates", new HashMap<>()).forEach((key, value) ->
                    ENERGY_GENERATION_RATES.put(new ResourceLocation(key), value)
            );

            LOGGER.info("Successfully loaded mana and energy generation rates from JSON.");
        } catch (Exception e) {
            LOGGER.error("Failed to load mana and energy generation rates from JSON", e);
        }
    }

    // 數據生成器部分
    public ManaGenerationRateLoader(PackOutput packOutput) {
        this.packOutput = packOutput; // 使用 PackOutput
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        Map<String, Map<String, Integer>> rates = new HashMap<>();

        Map<String, Integer> manaRates = new HashMap<>();
        manaRates.put("minecraft:lapis_lazuli", 100);
        manaRates.put("magical_industry:mana_dust", 150);
        manaRates.put("minecraft:coal", 50);
        rates.put("mana_rates", manaRates);

        Map<String, Integer> energyRates = new HashMap<>();
        energyRates.put("minecraft:coal", 200);
        energyRates.put("minecraft:charcoal", 150);
        rates.put("energy_rates", energyRates);

        Path outputPath = this.packOutput.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("magical_industry/mana_energy_generation_rates.json");

        return CompletableFuture.runAsync(() -> {
            DataProvider.saveStable(cachedOutput, GSON.toJsonTree(rates), outputPath);
            LOGGER.info("Successfully generated mana and energy generation rates JSON.");
        });

    }

    @Override
    public String getName() {
        return "Mana and Energy Generation Rate Provider";
    }

    // 註冊數據生成器
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        if (event.includeServer()) {
            generator.addProvider(true, new ManaGenerationRateLoader(packOutput));
        }
    }
}
