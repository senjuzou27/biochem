package ru.ufamod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.ufamod.UfaMod;
import ru.ufamod.entity.VehicleEntity;

/** Draws cars, minibuses and buses out of plain coloured boxes; the front of a vehicle looks along +Z. */
public class VehicleRenderer extends EntityRenderer<VehicleEntity> {
    private static final ResourceLocation WHITE = new ResourceLocation(UfaMod.MODID, "textures/entity/white.png");
    private static final int GLASS = 0x8FB7D8, TYRE = 0x1A1A1A, DARK = 0x2B2B2E, LAMP = 0xFFF3C0, TAIL = 0xC81E1E, STOP = 0xFF2A1A;

    public VehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 1.4f;
    }

    @Override
    public ResourceLocation getTextureLocation(VehicleEntity entity) {
        return WHITE;
    }

    @Override
    public void render(VehicleEntity e, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yRotO, e.getYRot())));
        VertexConsumer solid = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        int c = e.colour();
        int tail = e.braking() ? STOP : TAIL;
        switch (e.kind()) {
            case VehicleEntity.BUS -> bus(pose, solid, c, light, tail);
            case VehicleEntity.MINIBUS -> minibus(pose, solid, c, light, tail);
            default -> car(pose, solid, c, light, tail);
        }
        VertexConsumer glass = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        switch (e.kind()) {
            case VehicleEntity.BUS -> {
                band(pose, glass, 1.24f, 1.55f, 2.45f, -5.0f, 5.0f, light);
                box(pose, glass, -1.1f, 1.3f, 5.13f, 1.1f, 2.5f, 5.16f, GLASS, 46, light);
            }
            case VehicleEntity.MINIBUS -> {
                band(pose, glass, 1.0f, 1.3f, 2.0f, -2.9f, 1.9f, light);
                box(pose, glass, -0.9f, 1.3f, 2.72f, 0.9f, 2.05f, 2.75f, GLASS, 46, light);
            }
            default -> {
                band(pose, glass, 0.8f, 0.98f, 1.42f, -1.25f, 0.65f, light);
                box(pose, glass, -0.78f, 0.98f, 0.66f, 0.78f, 1.42f, 0.69f, GLASS, 46, light);
                box(pose, glass, -0.78f, 0.98f, -1.29f, 0.78f, 1.42f, -1.26f, GLASS, 46, light);
            }
        }
        pose.popPose();
        super.render(e, entityYaw, partial, pose, buffers, light);
    }

    // ------------------------------------------------------------------ models

    private static void wheels(PoseStack pose, VertexConsumer v, float half, float front, float back, float r, int light) {
        for (int side = -1; side <= 1; side += 2)
            for (float z : new float[]{front, back}) {
                float x1 = side < 0 ? -half - 0.02f : half - 0.25f;
                box(pose, v, x1, 0f, z - r, x1 + 0.27f, 2 * r, z + r, TYRE, 255, light);
            }
    }

    private static void car(PoseStack pose, VertexConsumer v, int c, int light, int tail) {
        wheels(pose, v, 0.85f, 1.3f, -1.3f, 0.32f, light);
        box(pose, v, -0.85f, 0.3f, -2.1f, 0.85f, 0.98f, 2.1f, c, 255, light);          // body
        box(pose, v, -0.8f, 1.42f, -1.3f, 0.8f, 1.5f, 0.7f, c, 255, light);            // roof
        for (int sx = -1; sx <= 1; sx += 2)
            for (float z : new float[]{-1.28f, -0.3f, 0.62f})                           // pillars
                box(pose, v, sx * 0.8f - 0.04f, 0.98f, z - 0.04f, sx * 0.8f + 0.04f, 1.42f, z + 0.04f, c, 255, light);
        box(pose, v, -0.75f, 0.6f, 2.1f, -0.4f, 0.8f, 2.13f, LAMP, 255, 0xF000F0);
        box(pose, v, 0.4f, 0.6f, 2.1f, 0.75f, 0.8f, 2.13f, LAMP, 255, 0xF000F0);
        box(pose, v, -0.75f, 0.65f, -2.13f, -0.4f, 0.85f, -2.1f, tail, 255, 0xF000F0);
        box(pose, v, 0.4f, 0.65f, -2.13f, 0.75f, 0.85f, -2.1f, tail, 255, 0xF000F0);
        box(pose, v, -0.86f, 0.3f, -2.14f, 0.86f, 0.45f, 2.14f, DARK, 255, light);     // bumpers and sills
    }

    private static void minibus(PoseStack pose, VertexConsumer v, int c, int light, int tail) {
        wheels(pose, v, 1.0f, 1.9f, -1.9f, 0.36f, light);
        box(pose, v, -1.0f, 0.35f, -3.0f, 1.0f, 1.3f, 2.2f, c, 255, light);            // body below the windows
        box(pose, v, -1.0f, 0.35f, 2.2f, 1.0f, 1.2f, 3.0f, c, 255, light);             // bonnet
        box(pose, v, -1.0f, 2.0f, -3.0f, 1.0f, 2.45f, 2.75f, c, 255, light);           // roof
        for (int sx = -1; sx <= 1; sx += 2)
            for (float z : new float[]{-2.96f, -1.4f, 0.2f, 1.86f, 2.7f})
                box(pose, v, sx * 1.0f - 0.05f, 1.3f, z - 0.05f, sx * 1.0f + 0.05f, 2.0f, z + 0.05f, c, 255, light);
        box(pose, v, -1.0f, 1.3f, -3.0f, 1.0f, 2.0f, -2.95f, c, 255, light);           // rear wall
        box(pose, v, -0.9f, 0.7f, 3.0f, -0.5f, 0.95f, 3.03f, LAMP, 255, 0xF000F0);
        box(pose, v, 0.5f, 0.7f, 3.0f, 0.9f, 0.95f, 3.03f, LAMP, 255, 0xF000F0);
        box(pose, v, -0.9f, 0.8f, -3.03f, -0.6f, 1.2f, -3.0f, tail, 255, 0xF000F0);
        box(pose, v, 0.6f, 0.8f, -3.03f, 0.9f, 1.2f, -3.0f, tail, 255, 0xF000F0);
        box(pose, v, -1.01f, 0.35f, -3.02f, 1.01f, 0.5f, 3.02f, DARK, 255, light);
    }

    private static void bus(PoseStack pose, VertexConsumer v, int c, int light, int tail) {
        wheels(pose, v, 1.24f, 3.3f, -3.0f, 0.5f, light);
        box(pose, v, -1.24f, 0.5f, -5.15f, 1.24f, 0.62f, 5.15f, DARK, 255, light);     // floor
        // walls below and above the window band; the inside stays hollow for the passengers
        for (int sx = -1; sx <= 1; sx += 2) {
            box(pose, v, sx * 1.24f - 0.03f, 0.62f, -5.15f, sx * 1.24f + 0.03f, 1.55f, 5.15f, c, 255, light);
            box(pose, v, sx * 1.24f - 0.03f, 2.45f, -5.15f, sx * 1.24f + 0.03f, 2.95f, 5.15f, c, 255, light);
            box(pose, v, sx * 1.24f - 0.035f, 0.62f, -5.15f, sx * 1.24f + 0.035f, 0.95f, 5.15f, 0x2F7A3A, 255, light);   // green stripe
            for (float z = -5.1f; z <= 5.11f; z += 1.7f)
                box(pose, v, sx * 1.24f - 0.05f, 1.55f, z - 0.06f, sx * 1.24f + 0.05f, 2.45f, z + 0.06f, c, 255, light);
        }
        box(pose, v, -1.24f, 2.95f, -5.15f, 1.24f, 3.05f, 5.15f, c, 255, light);        // roof
        box(pose, v, -1.24f, 0.62f, -5.15f, 1.24f, 2.95f, -5.1f, c, 255, light);        // rear
        box(pose, v, -1.24f, 0.62f, 5.1f, 1.24f, 1.3f, 5.15f, c, 255, light);           // front below the windscreen
        box(pose, v, -1.24f, 2.5f, 5.1f, 1.24f, 2.95f, 5.15f, DARK, 255, light);        // route board
        // seats along the walls
        for (int sx = -1; sx <= 1; sx += 2)
            for (float z = -4.2f; z <= 3.2f; z += 1.4f) {
                if (Math.abs(z) < 0.9f) continue;
                box(pose, v, sx * 0.7f - 0.4f, 0.62f, z - 0.25f, sx * 0.7f + 0.4f, 1.05f, z + 0.25f, 0x3B5BA5, 255, light);
                box(pose, v, sx * 0.7f - 0.4f, 1.05f, z - 0.25f, sx * 0.7f + 0.4f, 1.6f, z - 0.15f, 0x3B5BA5, 255, light);
            }
        box(pose, v, -1.1f, 0.75f, 5.15f, -0.6f, 1.0f, 5.18f, LAMP, 255, 0xF000F0);
        box(pose, v, 0.6f, 0.75f, 5.15f, 1.1f, 1.0f, 5.18f, LAMP, 255, 0xF000F0);
        box(pose, v, -1.1f, 0.9f, -5.18f, -0.8f, 1.4f, -5.15f, tail, 255, 0xF000F0);
        box(pose, v, 0.8f, 0.9f, -5.18f, 1.1f, 1.4f, -5.15f, tail, 255, 0xF000F0);
    }

    /** Side windows on both sides. */
    private static void band(PoseStack pose, VertexConsumer v, float half, float y1, float y2, float z1, float z2, int light) {
        box(pose, v, -half - 0.01f, y1, z1, -half + 0.01f, y2, z2, GLASS, 46, light);
        box(pose, v, half - 0.01f, y1, z1, half + 0.01f, y2, z2, GLASS, 46, light);
    }

    // ------------------------------------------------------------------ boxes

    private static void box(PoseStack pose, VertexConsumer v, float x1, float y1, float z1, float x2, float y2, float z2, int rgb, int alpha, int light) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        quad(v, m, n, r, g, b, alpha, light, 0, 1, 0, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        quad(v, m, n, r, g, b, alpha, light, 0, -1, 0, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2);
        quad(v, m, n, r, g, b, alpha, light, 0, 0, 1, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
        quad(v, m, n, r, g, b, alpha, light, 0, 0, -1, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1);
        quad(v, m, n, r, g, b, alpha, light, 1, 0, 0, x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2);
        quad(v, m, n, r, g, b, alpha, light, -1, 0, 0, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
    }

    private static void quad(VertexConsumer v, Matrix4f m, Matrix3f n, int r, int g, int b, int a, int light, float nx, float ny, float nz, float... p) {
        for (int i = 0; i < 4; i++)
            v.vertex(m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(n, nx, ny, nz).endVertex();
    }
}
