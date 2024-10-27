package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.client.model.ManaGeneratorBlockItemModel;
import com.github.nalamodikk.common.block.custom.blockitem.ManaGeneratorBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ManaGeneratorBlockItemRenderer extends GeoItemRenderer<ManaGeneratorBlockItem> {
    public ManaGeneratorBlockItemRenderer() {
        super(new ManaGeneratorBlockItemModel());
    }
}