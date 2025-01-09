package com.github.nalamodikk.common.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RenderUtils {

    /**
     * 將 VoxelShape 渲染到對應的位置。
     *
     * @param poseStack      當前的 PoseStack。
     * @param vertexConsumer 目標 VertexConsumer。
     * @param shape          要渲染的 VoxelShape。
     * @param light          光照值。
     * @param overlay        覆蓋層。
     */
    public static void renderShape(PoseStack poseStack, VertexConsumer vertexConsumer, VoxelShape shape, int light, int overlay) {
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            renderBox(poseStack, vertexConsumer, minX, minY, minZ, maxX, maxY, maxZ, light, overlay);
        });
    }

    /**
     * 渲染一個方塊 AABB。
     *
     * @param poseStack      當前的 PoseStack。
     * @param vertexConsumer 目標 VertexConsumer。
     * @param minX           AABB 的最小 X。
     * @param minY           AABB 的最小 Y。
     * @param minZ           AABB 的最小 Z。
     * @param maxX           AABB 的最大 X。
     * @param maxY           AABB 的最大 Y。
     * @param maxZ           AABB 的最大 Z。
     * @param light          光照值。
     * @param overlay        覆蓋層。
     */
    public static void renderBox(PoseStack poseStack, VertexConsumer vertexConsumer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int light, int overlay) {
        // 顯示每個面的頂點（6 個面）
        addVertex(poseStack, vertexConsumer, minX, minY, minZ, light, overlay); // 底部左前
        addVertex(poseStack, vertexConsumer, maxX, minY, minZ, light, overlay); // 底部右前
        addVertex(poseStack, vertexConsumer, maxX, maxY, minZ, light, overlay); // 頂部右前
        addVertex(poseStack, vertexConsumer, minX, maxY, minZ, light, overlay); // 頂部左前

        addVertex(poseStack, vertexConsumer, minX, minY, maxZ, light, overlay); // 底部左後
        addVertex(poseStack, vertexConsumer, maxX, minY, maxZ, light, overlay); // 底部右後
        addVertex(poseStack, vertexConsumer, maxX, maxY, maxZ, light, overlay); // 頂部右後
        addVertex(poseStack, vertexConsumer, minX, maxY, maxZ, light, overlay); // 頂部左後
    }

    private static void addVertex(PoseStack poseStack, VertexConsumer vertexConsumer, double x, double y, double z, int light, int overlay) {
        vertexConsumer.vertex(poseStack.last().pose(), (float) x, (float) y, (float) z)
                .color(255, 255, 255, 255) // 默認白色
                .uv(0, 0) // 貼圖座標
                .overlayCoords(overlay)
                .uv2(light)
                .normal(0, 1, 0) // 默認法線
                .endVertex();
    }
}
