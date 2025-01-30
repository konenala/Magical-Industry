package com.github.nalamodikk.common.register;


import com.github.nalamodikk.client.renderer.ManaGeneratorRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.fml.common.Mod;

public class ModRenderers {
    public static void registerBlockEntityRenderers() {
        BlockEntityRenderers.register(ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorRenderer::new);
//        BlockEntityRenderers.register(ModBlockEntities.MANA_CONDUIT_BE.get(), ManaConduitRenderer::new);
    }
}
