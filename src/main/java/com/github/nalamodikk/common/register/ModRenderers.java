package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.block.entity.ModBlockEntities;
import com.github.nalamodikk.client.renderer.ManaGeneratorRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class ModRenderers {
    public static void registerBlockEntityRenderers() {
        BlockEntityRenderers.register(ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorRenderer::new);
    }
}
