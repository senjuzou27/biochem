package ru.ufamod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.ufamod.UfaMod;
import ru.ufamod.entity.VehicleEntity;

/**
 * Cars, the minibus and the city bus, modelled from side profiles extruded across the width, with windows,
 * turning wheels, lights, bumpers and plates. The front of a vehicle looks along +Z, its right side is -X.
 * Far away a vehicle is drawn as a few boxes only.
 */
public class VehicleRenderer extends EntityRenderer<VehicleEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(UfaMod.MODID, "textures/entity/white.png");
    static final int FULL = 0xF000F0;
    static final int GLASS = 0x1E2A33, GLASS_LIGHT = 0x6E8FA6, TYRE = 0x161616, RIM = 0xB9BDC2, HUB = 0x55595E, PLASTIC = 0x2A2B2D,
            CHROME = 0xD0D4D8, LAMP = 0xFFF6D8, TAIL = 0x9A1010, TAIL_ON = 0xFF2A10, AMBER = 0xFF9A1A, PLATE = 0xEDEDED, SEAT = 0x2E4E9E,
            RAIL = 0xF2C230, FLOOR = 0x4A4C50, ARCH = 0x0E0E0E;

    public VehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 1.3f;
    }

    @Override
    public ResourceLocation getTextureLocation(VehicleEntity entity) {
        return WHITE;
    }

    @Override
    public void render(VehicleEntity e, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yRotO, e.getYRot())));
        B b = new B(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE)), light);
        boolean near = entityRenderDispatcher.distanceToSqr(e) < 72 * 72;
        float spin = Mth.lerp(partial, e.wheelSpinO, e.wheelSpin);
        int c = e.colour();
        switch (e.kind()) {
            case VehicleEntity.BUS -> {
                if (near) bus(b, e, spin);
                else b.box(-1.25f, 0.35f, -5.3f, 1.25f, 3.05f, 5.3f, 0xEDEDED);
            }
            case VehicleEntity.MINIBUS -> {
                if (near) minibus(b, c, e.braking(), spin);
                else b.box(-1.0f, 0.35f, -3.2f, 1.0f, 2.6f, 3.1f, c);
            }
            default -> {
                int style = e.style();
                if (near) car(b, style, style == 9 && !e.parked() ? 0xF2C318 : c, e.braking() && !e.parked(), e.parked(), spin);
                else b.box(-0.87f, 0.32f, -2.2f, 0.87f, 1.45f, 2.2f, c);
            }
        }
        if (near) {
            // the glass goes last, translucent
            B g = new B(pose, buffers.getBuffer(RenderType.entityTranslucent(WHITE)), light);
            switch (e.kind()) {
                case VehicleEntity.BUS -> busGlass(g);
                case VehicleEntity.MINIBUS -> minibusGlass(g);
                default -> carGlass(g, e.style());
            }
            if (e.kind() == VehicleEntity.BUS) routeBoard(pose, buffers, e, 5.32f, 2.79f, 1.0f, true);
            if (e.kind() == VehicleEntity.MINIBUS) routeBoard(pose, buffers, e, 2.42f, 2.28f, 0.6f, false);
        }
        pose.popPose();
        super.render(e, entityYaw, partial, pose, buffers, light);
    }

    // ------------------------------------------------------------------ cars

    /** Side profiles (z, y) of the body and of the cabin for the four kinds of cars. */
    private static final float[][] SEDAN_BODY = {{-2.22f, 0.30f}, {2.20f, 0.30f}, {2.27f, 0.58f}, {2.17f, 0.84f}, {1.05f, 0.98f}, {-1.45f, 0.99f}, {-2.17f, 0.95f}, {-2.27f, 0.62f}};
    private static final float[][] SEDAN_CAB = {{-1.40f, 0.98f}, {1.00f, 0.98f}, {0.30f, 1.46f}, {-0.85f, 1.48f}};
    private static final float[][] HATCH_BODY = {{-1.95f, 0.30f}, {1.98f, 0.30f}, {2.05f, 0.58f}, {1.95f, 0.84f}, {0.95f, 0.98f}, {-1.80f, 0.99f}, {-2.00f, 0.62f}};
    private static final float[][] HATCH_CAB = {{-1.85f, 0.98f}, {0.90f, 0.98f}, {0.25f, 1.46f}, {-1.62f, 1.46f}};
    private static final float[][] SUV_BODY = {{-2.15f, 0.40f}, {2.15f, 0.40f}, {2.24f, 0.70f}, {2.12f, 1.02f}, {1.05f, 1.15f}, {-1.85f, 1.16f}, {-2.20f, 0.74f}};
    private static final float[][] SUV_CAB = {{-1.95f, 1.15f}, {0.98f, 1.15f}, {0.25f, 1.68f}, {-1.80f, 1.70f}};

    private static float[][] body(int style) {
        return style == 5 || style == 6 ? HATCH_BODY : style == 7 || style == 8 ? SUV_BODY : SEDAN_BODY;
    }

    private static float[][] cab(int style) {
        return style == 5 || style == 6 ? HATCH_CAB : style == 7 || style == 8 ? SUV_CAB : SEDAN_CAB;
    }

    private static void car(B b, int style, int c, boolean braking, boolean parked, float spin) {
        float[][] body = body(style), cab = cab(style);
        boolean suv = style == 7 || style == 8;
        float hw = suv ? 0.9f : 0.87f, cw = hw - 0.13f;
        float wr = suv ? 0.37f : 0.32f, wy = wr, front = suv ? 1.38f : style == 5 || style == 6 ? 1.22f : 1.32f, rear = -front - (style == 5 || style == 6 ? 0.0f : 0.02f);
        float zMin = body[0][0], zMax = body[1][0];
        // dark wheel arches, then the body around them
        for (float wz : new float[]{front, rear})
            for (int s = -1; s <= 1; s += 2) b.disc(s * (hw + 0.004f), wy, wz, wr + 0.07f, ARCH, 14, 0, s);
        b.prism(-hw, hw, body, c, null);
        // the cabin: roof here, glass later; the sides are only pillars around the windows
        b.prism(-cw, cw, cab, c, new int[]{-1, -2, c, -2}, false);
        float split0 = (cab[0][0] + cab[1][0]) / 2f + 0.15f;
        for (int s = -1; s <= 1; s += 2) {
            float x = s * cw;
            float top = cab[2][1];
            b.poly(x, new float[][]{cab[1], cab[2], {cab[2][0] - 0.09f, cab[2][1]}, {cab[1][0] - 0.14f, cab[1][1]}}, c);                 // A pillar
            b.poly(x, new float[][]{{cab[0][0] + 0.16f, cab[0][1]}, {cab[3][0] + 0.1f, cab[3][1]}, cab[3], cab[0]}, c);                    // C pillar
            b.side(x, top - 0.06f, cab[3][0], top, cab[2][0], c);                                                                         // roof rail
            b.side(x, cab[0][1], split0 - 0.06f, top, split0 + 0.06f, c);                                                                  // B pillar
            b.side(x, cab[0][1], cab[0][0], cab[0][1] + 0.05f, cab[1][0], c);                                                             // belt line
        }
        // the inside: dark trim, seat backs and head rests seen through the glass
        int trim = 0x26272A;
        b.box(-cw + 0.03f, cab[0][1] - 0.02f, cab[0][0] + 0.05f, cw - 0.03f, cab[0][1] + 0.01f, cab[1][0] - 0.05f, trim);
        b.box(-cw + 0.03f, cab[2][1] - 0.03f, cab[3][0] + 0.02f, cw - 0.03f, cab[2][1] - 0.01f, cab[2][0] - 0.02f, 0x6A6C70);
        for (int s = -1; s <= 1; s += 2) {
            b.box(s * 0.12f, cab[0][1], split0 - 0.18f, s * 0.6f, cab[0][1] + 0.38f, split0 - 0.08f, trim);
            b.box(s * 0.25f, cab[0][1] + 0.38f, split0 - 0.17f, s * 0.47f, cab[0][1] + 0.5f, split0 - 0.09f, trim);
        }
        b.box(-cw + 0.08f, cab[0][1], cab[0][0] + 0.25f, cw - 0.08f, cab[0][1] + 0.36f, cab[0][0] + 0.38f, trim);
        b.box(-0.55f, cab[0][1], cab[1][0] - 0.30f, 0.55f, cab[0][1] + 0.1f, cab[1][0] - 0.12f, trim);
        // lower sills and bumpers in dark plastic
        b.box(-hw - 0.01f, 0.30f, zMin + 0.35f, -hw + 0.03f, 0.40f, zMax - 0.35f, PLASTIC);
        b.box(hw - 0.03f, 0.30f, zMin + 0.35f, hw + 0.01f, 0.40f, zMax - 0.35f, PLASTIC);
        b.box(-hw + 0.05f, 0.30f, zMax - 0.02f, hw - 0.05f, 0.46f, zMax + 0.06f, PLASTIC);
        b.box(-hw + 0.05f, 0.30f, zMin - 0.06f, hw - 0.05f, 0.46f, zMin + 0.02f, PLASTIC);
        // grille, headlights, indicators, fog lights
        float fz = zMax + 0.065f;
        b.box(-0.42f, 0.50f, fz - 0.06f, 0.42f, 0.68f, fz, PLASTIC);
        for (int s = -1; s <= 1; s += 2) {
            b.boxLit(s * 0.48f, 0.66f, fz - 0.10f, s * 0.80f, 0.78f, fz - 0.05f, LAMP, FULL);
            b.boxLit(s * 0.80f, 0.66f, fz - 0.12f, s * 0.86f, 0.74f, fz - 0.07f, AMBER, FULL);
            b.boxLit(s * 0.55f, 0.36f, fz, s * 0.68f, 0.42f, fz + 0.01f, LAMP, FULL);
        }
        // plates
        b.box(-0.26f, 0.38f, fz, 0.26f, 0.49f, fz + 0.012f, PLATE);
        b.box(-0.26f, 0.52f, zMin - 0.075f, 0.26f, 0.63f, zMin - 0.063f, PLATE);
        // tail lights, brighter when braking
        float rz = zMin - 0.04f;
        for (int s = -1; s <= 1; s += 2) {
            b.boxLit(s * 0.55f, 0.66f, rz - 0.02f, s * 0.86f, 0.82f, rz + 0.04f, braking ? TAIL_ON : TAIL, braking ? FULL : b.light);
            b.boxLit(s * 0.60f, 0.62f, rz - 0.02f, s * 0.80f, 0.655f, rz + 0.04f, AMBER, b.light);
        }
        // door lines, handles and mirrors on both sides
        float split = (cab[0][0] + cab[1][0]) / 2f + 0.15f;
        for (int s = -1; s <= 1; s += 2) {
            float x = s * (hw + 0.006f);
            b.box(x - 0.004f, 0.42f, split - 0.012f, x + 0.004f, body[4][1] - 0.02f, split + 0.012f, shade(c, 0.55f));
            b.box(x - 0.004f, 0.42f, cab[1][0] - 0.05f, x + 0.004f, body[4][1] - 0.02f, cab[1][0] - 0.026f, shade(c, 0.55f));
            if (style != 5 && style != 6 || true)
                b.box(x - 0.004f, 0.42f, cab[0][0] + 0.1f, x + 0.004f, body[4][1] - 0.02f, cab[0][0] + 0.124f, shade(c, 0.55f));
            b.box(x - 0.01f * s, body[4][1] - 0.16f, split + 0.25f, x + 0.03f * s, body[4][1] - 0.12f, split + 0.42f, CHROME);
            b.box(x - 0.01f * s, body[4][1] - 0.16f, cab[0][0] + 0.38f, x + 0.03f * s, body[4][1] - 0.12f, cab[0][0] + 0.55f, CHROME);
            // mirror on a short arm
            float mz = cab[1][0] - 0.12f, my = cab[1][1] + 0.06f;
            b.box(s * cw, my, mz - 0.05f, s * (hw + 0.14f), my + 0.04f, mz, PLASTIC);
            b.box(s * (hw + 0.06f), my - 0.02f, mz - 0.12f, s * (hw + 0.17f), my + 0.13f, mz - 0.02f, c);
        }
        if (suv) {
            // roof rails
            for (int s = -1; s <= 1; s += 2) b.box(s * (cw - 0.12f), cab[2][1], cab[3][0] + 0.1f, s * (cw - 0.06f), cab[2][1] + 0.06f, cab[2][0] - 0.1f, PLASTIC);
        }
        if (style == 9 && !parked) {
            // a taxi: a lit sign on the roof with checkers
            float y = cab[2][1];
            b.box(-0.30f, y, -0.40f, 0.30f, y + 0.02f, -0.20f, PLASTIC);
            b.boxLit(-0.28f, y + 0.02f, -0.36f, 0.28f, y + 0.20f, -0.24f, 0xFFE070, FULL);
            for (int i = 0; i < 4; i++) {
                float x0 = -0.28f + i * 0.14f;
                b.box(x0, y + 0.13f, -0.365f, x0 + 0.07f, y + 0.20f, -0.235f, 0x111111);
                b.box(x0 + 0.07f, y + 0.06f, -0.365f, x0 + 0.14f, y + 0.13f, -0.235f, 0x111111);
            }
        }
        // a short aerial
        if (style % 3 == 0) b.box(-0.02f, cab[2][1], cab[3][0] + 0.05f, 0.02f, cab[2][1] + 0.25f, cab[3][0] + 0.09f, PLASTIC);
        for (float wz : new float[]{front, rear})
            for (int s = -1; s <= 1; s += 2) wheel(b, s, hw - 0.17f, hw + 0.01f, wy, wz, wr, spin);
    }

    private static void carGlass(B g, int style) {
        float[][] body = body(style), cab = cab(style);
        boolean suv = style == 7 || style == 8;
        float hw = suv ? 0.9f : 0.87f, cw = hw - 0.13f;
        // windscreen and rear window on the slopes of the cabin
        g.slope(-cw + 0.06f, cw - 0.06f, cab[1], cab[2], 0.04f, GLASS, 170);
        g.slope(-cw + 0.06f, cw - 0.06f, cab[3], cab[0], 0.04f, GLASS, 170);
        // side windows with a pillar between them
        float split = (cab[0][0] + cab[1][0]) / 2f + 0.15f;
        float yb = cab[0][1] + 0.05f, yt = cab[2][1] - 0.06f;
        for (int s = -1; s <= 1; s += 2) {
            float x = s * (cw + 0.006f);
            g.sideWindow(x, s, cab, cab[0][0] + 0.12f, split - 0.06f, yb, yt, GLASS, 140);
            g.sideWindow(x, s, cab, split + 0.06f, cab[1][0] - 0.08f, yb, yt, GLASS, 140);
        }
    }

    // ------------------------------------------------------------------ the minibus (a GAZelle Next)

    private static final float[][] MINI = {{-3.20f, 0.35f}, {3.00f, 0.35f}, {3.14f, 0.60f}, {3.06f, 1.00f}, {2.55f, 1.32f}, {2.05f, 2.42f}, {1.70f, 2.58f}, {-3.10f, 2.58f}, {-3.20f, 2.45f}};

    private static void minibus(B b, int c, boolean braking, float spin) {
        float hw = 1.02f;
        for (float wz : new float[]{2.15f, -1.75f})
            for (int s = -1; s <= 1; s += 2) b.disc(s * (hw + 0.004f), 0.37f, wz, 0.45f, ARCH, 14, 0, s);
        b.prism(-hw, hw, MINI, c, new int[]{-1, -1, -1, -1, -2, -1, -1, -1, -1}, false);
        for (int s = -1; s <= 1; s += 2) {
            float x = s * hw;
            // under the windows, the cab window frame, above the windows; pillars between the windows
            b.poly(x, new float[][]{{-3.2f, 0.35f}, {3.0f, 0.35f}, {3.14f, 0.6f}, {3.06f, 1.0f}, {2.55f, 1.32f}, {2.52f, 1.42f}, {-3.2f, 1.42f}}, c);
            b.poly(x, new float[][]{{-3.2f, 2.24f}, {2.13f, 2.24f}, {2.05f, 2.42f}, {1.7f, 2.58f}, {-3.1f, 2.58f}, {-3.2f, 2.45f}}, c);
            b.poly(x, new float[][]{{1.92f, 1.42f}, {2.52f, 1.42f}, {2.13f, 2.24f}, {1.92f, 2.24f}}, c);
            for (float z = -3.2f; z < 1.95f; z += 1.0f) b.side(x, 1.42f, z, 2.24f, z + 0.1f, PLASTIC);
        }
        // the back with a window
        b.across(-hw, 0.35f, -3.2f, hw, 1.45f, c);
        b.across(-hw, 2.30f, -3.2f, hw, 2.45f, c);
        for (int s = -1; s <= 1; s += 2) b.side(s * (hw - 0.05f), 1.45f, -3.2f, 2.30f, -3.18f, c);
        // a darker skirt and bumpers
        b.box(-hw - 0.01f, 0.35f, -3.15f, hw + 0.01f, 0.55f, 3.05f, shade(c, 0.85f));
        b.box(-hw + 0.04f, 0.33f, 2.98f, hw - 0.04f, 0.55f, 3.18f, PLASTIC);
        b.box(-hw + 0.04f, 0.33f, -3.26f, hw - 0.04f, 0.55f, -3.14f, PLASTIC);
        b.box(-0.5f, 0.62f, 3.10f, 0.5f, 0.88f, 3.14f, PLASTIC);
        for (int s = -1; s <= 1; s += 2) {
            b.boxLit(s * 0.55f, 0.82f, 3.03f, s * 0.95f, 0.98f, 3.10f, LAMP, FULL);
            b.boxLit(s * 0.92f, 0.70f, 3.05f, s * 0.99f, 0.80f, 3.11f, AMBER, FULL);
            b.boxLit(s * 0.80f, 0.70f, -3.25f, s * 0.98f, 1.30f, -3.18f, braking ? TAIL_ON : TAIL, braking ? FULL : b.light);
            // mirrors on long arms, as on a van
            b.box(s * hw, 1.75f, 2.30f, s * (hw + 0.22f), 1.79f, 2.36f, PLASTIC);
            b.box(s * (hw + 0.16f), 1.45f, 2.26f, s * (hw + 0.24f), 1.85f, 2.34f, PLASTIC);
        }
        b.box(-0.26f, 0.38f, 3.18f, 0.26f, 0.49f, 3.19f, PLATE);
        b.box(-0.26f, 0.62f, -3.27f, 0.26f, 0.73f, -3.26f, PLATE);
        // the sliding door on the right side
        for (int s = -1; s <= 1; s += 2) {
            float x = s * (hw + 0.004f);
            if (s < 0) {
                b.box(x - 0.008f, 0.45f, 0.55f, x + 0.008f, 2.30f, 0.58f, shade(c, 0.5f));
                b.box(x - 0.008f, 0.45f, 1.58f, x + 0.008f, 2.30f, 1.61f, shade(c, 0.5f));
                b.box(x - 0.012f, 1.10f, 0.62f, x + 0.012f, 1.14f, 0.85f, CHROME);
                b.box(x - 0.004f, 0.18f, 0.60f, x + 0.004f, 0.36f, 1.55f, PLASTIC);
            }
        }
        // the yellow route board on the right side
        b.boxLit(-hw - 0.02f, 1.62f, 1.2f, -hw - 0.01f, 1.85f, 1.9f, 0xF0D040, b.light);
        // inside: seats in rows
        b.box(-0.95f, 0.36f, -3.1f, 0.95f, 0.40f, 2.0f, FLOOR);
        for (float z = -2.8f; z < 0.4f; z += 0.85f)
            for (float x : new float[]{-0.85f, -0.4f, 0.45f})
                seat(b, x, x + 0.38f, z, 0.4f);
        for (float wz : new float[]{2.15f, -1.75f})
            for (int s = -1; s <= 1; s += 2) wheel(b, s, hw - 0.2f, hw + 0.01f, 0.37f, wz, 0.37f, spin);
    }

    private static void minibusGlass(B g) {
        float hw = 1.02f;
        g.slope(-hw + 0.05f, hw - 0.05f, MINI[4], MINI[5], 0.03f, GLASS, 160);
        for (int s = -1; s <= 1; s += 2) {
            float x = s * (hw + 0.012f);
            for (float z = -2.9f; z < 1.9f; z += 1.0f) {
                float z2 = Math.min(z + 0.9f, 1.92f);
                if (s < 0 && z2 > 0.55f && z < 1.6f) continue;
                g.flat(x, 1.42f, z + 0.1f, 2.24f, z2, s, GLASS, 130);
            }
            if (s < 0) g.flat(x, 1.0f, 0.62f, 2.24f, 1.55f, s, GLASS, 165);
            // the cab window under the slope
            g.flat(x, 1.42f, 1.98f, 2.10f, 2.35f, s, GLASS, 165);
        }
        g.flat2(-hw + 0.1f, 1.45f, -3.215f, hw - 0.1f, 2.3f, GLASS, 165);
    }

    // ------------------------------------------------------------------ the city bus (a low-floor NefAZ)

    private static final float[][] BUS = {{-5.30f, 0.32f}, {5.22f, 0.32f}, {5.32f, 0.55f}, {5.32f, 2.92f}, {5.18f, 3.05f}, {-5.18f, 3.05f}, {-5.30f, 2.92f}};
    private static final float[][] DOORS = {{4.05f, 5.0f}, {0.0f, 1.25f}, {-3.85f, -2.65f}};

    private static void bus(B b, VehicleEntity e, float spin) {
        float hw = 1.25f;
        int white = 0xEDEDED, green = 0x2F7A3A, blue = 0x1F4E9E;
        for (float wz : new float[]{3.35f, -2.95f})
            for (int s = -1; s <= 1; s += 2) b.disc(s * (hw + 0.004f), 0.5f, wz, 0.6f, ARCH, 16, 0, s);
        b.prism(-hw, hw, BUS, white, new int[]{-1, -1, -2, -1, -1, -1, -2}, false);
        for (int s = -1; s <= 1; s += 2) {
            float x = s * hw;
            // the wall under the windows, except where the doors are on the right side
            float z = -5.30f;
            if (s < 0) for (float[] d : DOORS) {
                b.side(x, 0.32f, z, 1.30f, d[0] - 0.05f, white);
                z = d[1] + 0.05f;
            }
            b.side(x, 0.32f, z, 1.30f, 5.32f, white);
            b.side(x, 2.60f, -5.30f, 2.92f, 5.32f, white);
            b.poly(x, new float[][]{{5.32f, 2.92f}, {5.18f, 3.05f}, {-5.18f, 3.05f}, {-5.30f, 2.92f}}, white);
            b.poly(x, new float[][]{{5.22f, 0.32f}, {5.32f, 0.55f}, {5.32f, 1.30f}, {5.22f, 1.30f}}, white);
        }
        // front: the panel under the windscreen and the board above it; back: under and above the window
        b.across(-hw, 0.55f, 5.32f, hw, 1.02f, white);
        b.across(-hw, 2.62f, 5.32f, hw, 2.92f, white);
        b.across(-hw, 0.32f, -5.30f, hw, 1.50f, white);
        b.across(-hw, 2.60f, -5.30f, hw, 2.92f, white);
        for (int s = -1; s <= 1; s += 2) {
            b.side(s * (hw - 0.07f), 1.50f, -5.30f, 2.60f, -5.24f, white);
            b.side(s * (hw - 0.04f), 1.02f, 5.26f, 2.62f, 5.32f, PLASTIC);
        }
        // livery: a green skirt and a blue stripe under the windows
        for (int s = -1; s <= 1; s += 2) {
            float x = s * (hw + 0.005f);
            b.box(x - 0.004f, 0.34f, -5.25f, x + 0.004f, 0.95f, 5.25f, green);
            b.box(x - 0.004f, 1.12f, -5.25f, x + 0.004f, 1.22f, 5.25f, blue);
            // black pillars of the window band
            b.box(x - 0.006f, 1.30f, -5.25f, x + 0.006f, 1.34f, 5.25f, PLASTIC);
            b.box(x - 0.006f, 2.60f, -5.25f, x + 0.006f, 2.66f, 5.25f, PLASTIC);
            for (float z = -5.0f; z <= 5.01f; z += 1.25f) b.box(x - 0.006f, 1.30f, z - 0.05f, x + 0.006f, 2.66f, z + 0.05f, PLASTIC);
        }
        // doors on the right side: dark frames, a black seal down the middle
        for (float[] d : DOORS) {
            float x = -hw - 0.008f;
            b.box(x - 0.004f, 0.35f, d[0] - 0.05f, x + 0.004f, 2.70f, d[0], PLASTIC);
            b.box(x - 0.004f, 0.35f, d[1], x + 0.004f, 2.70f, d[1] + 0.05f, PLASTIC);
            b.box(x - 0.004f, 2.62f, d[0], x + 0.004f, 2.70f, d[1], PLASTIC);
            b.box(x - 0.006f, 0.35f, (d[0] + d[1]) / 2 - 0.02f, x + 0.006f, 2.62f, (d[0] + d[1]) / 2 + 0.02f, PLASTIC);
        }
        // front: bumper, headlights, indicators, the black frame of the windscreen
        b.box(-hw, 0.30f, 5.28f, hw, 0.62f, 5.40f, PLASTIC);
        for (int s = -1; s <= 1; s += 2) {
            b.boxLit(s * 0.75f, 0.70f, 5.30f, s * 1.18f, 0.88f, 5.36f, LAMP, FULL);
            b.boxLit(s * 1.05f, 0.92f, 5.30f, s * 1.18f, 1.00f, 5.36f, AMBER, FULL);
            b.boxLit(s * 0.95f, 0.70f, -5.38f, s * 1.18f, 1.25f, -5.31f, e.braking() ? TAIL_ON : TAIL, e.braking() ? FULL : b.light);
            // mirrors on arms that hang down in front
            b.box(s * hw, 2.55f, 5.10f, s * (hw + 0.30f), 2.60f, 5.16f, PLASTIC);
            b.box(s * (hw + 0.25f), 2.05f, 5.20f, s * (hw + 0.32f), 2.55f, 5.30f, PLASTIC);
        }
        b.box(-hw + 0.05f, 0.95f, 5.32f, hw - 0.05f, 1.02f, 5.34f, PLASTIC);
        b.box(-hw + 0.05f, 2.62f, 5.32f, hw - 0.05f, 2.98f, 5.34f, 0x111111);
        b.box(-0.26f, 0.40f, 5.40f, 0.26f, 0.52f, 5.41f, PLATE);
        // rear: the engine grille and a plate
        b.box(-hw + 0.1f, 0.40f, -5.38f, hw - 0.1f, 1.20f, -5.31f, PLASTIC);
        b.box(-0.26f, 1.30f, -5.32f, 0.26f, 1.42f, -5.31f, PLATE);
        // roof: the air conditioning unit
        b.box(-0.9f, 3.05f, -1.2f, 0.9f, 3.32f, 1.6f, 0xDADADA);
        b.box(-0.7f, 3.32f, -0.8f, 0.7f, 3.34f, 1.2f, PLASTIC);
        // inside: floor, blue seats, yellow handrails
        b.box(-1.18f, 0.35f, -5.2f, 1.18f, 0.40f, 5.2f, FLOOR);
        for (float z = -4.9f; z <= 3.4f; z += 0.85f) {
            if (z > -0.2f && z < 1.4f || z > -4.0f && z < -2.5f) continue;
            seat(b, 0.35f, 0.75f, z, 0.4f);
            seat(b, 0.78f, 1.18f, z, 0.4f);
            if (z < -3.0f || z > 1.6f) seat(b, -1.15f, -0.75f, z, 0.4f);
        }
        // handrails along the aisle, poles by the seats, light strips in the ceiling
        for (int s = -1; s <= 1; s += 2) {
            b.box(s * 0.55f - 0.025f, 2.55f, -5.0f, s * 0.55f + 0.025f, 2.6f, 4.8f, RAIL);
            for (float z = -4.5f; z <= 3.5f; z += 1.7f) b.box(s * 0.33f - 0.025f, 0.40f, z - 0.025f, s * 0.33f + 0.025f, 2.58f, z + 0.025f, RAIL);
            b.boxLit(s * 0.95f - 0.12f, 2.98f, -4.8f, s * 0.95f + 0.12f, 3.0f, 4.6f, 0xF4F1E6, FULL);
        }
        // the driver's seat and a dashboard
        seat(b, 0.4f, 0.85f, 4.35f, 0.4f);
        b.box(-1.1f, 0.4f, 4.95f, 1.1f, 1.05f, 5.25f, PLASTIC);
        for (float wz : new float[]{3.35f, -2.95f})
            for (int s = -1; s <= 1; s += 2) wheel(b, s, hw - 0.28f, hw + 0.01f, 0.5f, wz, 0.5f, spin);
    }

    private static void busGlass(B g) {
        float hw = 1.25f;
        for (int s = -1; s <= 1; s += 2) {
            float x = s * (hw + 0.012f);
            for (float z = -5.0f; z < 5.0f; z += 1.25f) g.flat(x, 1.34f, z + 0.05f, 2.60f, z + 1.2f, s, GLASS, 150);
            if (s < 0) for (float[] d : DOORS) g.flat(x - 0.004f, 0.40f, d[0], 1.30f, d[1], s, GLASS, 150);
        }
        g.flat2(-hw + 0.05f, 1.02f, 5.335f, hw - 0.05f, 2.62f, GLASS_LIGHT, 120);
        g.flat2(-hw + 0.15f, 1.5f, -5.315f, hw - 0.15f, 2.6f, GLASS, 160);
    }

    /** The lit route board: number and destination in amber letters. */
    private void routeBoard(PoseStack pose, MultiBufferSource buffers, VehicleEntity e, float z, float y, float width, boolean bus) {
        Font font = Minecraft.getInstance().font;
        String number = e.routeNumber(), dest = bus ? e.routeName() : "";
        pose.pushPose();
        pose.translate(0f, y, z + 0.012f);
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        float scale = bus ? 0.0125f : 0.012f;
        pose.scale(-scale, -scale, scale);
        Matrix4f m = pose.last().pose();
        int amber = 0xFFB020;
        float numberW = font.width(number);
        if (bus) {
            float total = numberW + 6 + font.width(dest);
            float fit = Math.min(1f, (width * 2 / scale - 8) / total);
            pose.scale(fit, fit, fit);
            m = pose.last().pose();
            float x0 = -total / 2;
            font.drawInBatch(number, x0, -4, amber, false, m, buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FULL);
            font.drawInBatch(dest, x0 + numberW + 6, -4, amber, false, m, buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FULL);
        } else font.drawInBatch(number, -numberW / 2, -4, 0x111111, false, m, buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FULL);
        pose.popPose();
    }

    // ------------------------------------------------------------------ parts

    /** A wheel: tyre, rim with spokes that turn, a hub. s = side (-1 right, 1 left). */
    private static void wheel(B b, int s, float xin, float xout, float y, float z, float r, float spin) {
        float x1 = s * xin, x2 = s * xout;
        b.cylX(Math.min(x1, x2), Math.max(x1, x2), y, z, r, 16, TYRE, spin);
        float face = s * (xout + 0.004f);
        b.disc(face, y, z, r * 0.68f, RIM, 12, spin, s);
        for (int k = 0; k < 5; k++) {
            float a = spin + k * Mth.TWO_PI / 5f;
            b.spoke(face + s * 0.002f, y, z, r * 0.18f, r * 0.62f, a, 0.06f * r / 0.32f, HUB, s);
        }
        b.disc(face + s * 0.004f, y, z, r * 0.17f, HUB, 8, spin, s);
    }

    private static void seat(B b, float x1, float x2, float z, float floor) {
        b.box(x1, floor, z - 0.2f, x2, floor + 0.42f, z + 0.2f, SEAT);
        b.box(x1, floor + 0.42f, z - 0.2f, x2, floor + 0.95f, z - 0.1f, SEAT);
    }

    static int shade(int rgb, float f) {
        int r = Math.min(255, (int) ((rgb >> 16 & 255) * f)), g = Math.min(255, (int) ((rgb >> 8 & 255) * f)), bl = Math.min(255, (int) ((rgb & 255) * f));
        return r << 16 | g << 8 | bl;
    }

    // ------------------------------------------------------------------ the geometry builder

    private static final class B {
        final Matrix4f m;
        final Matrix3f n;
        final VertexConsumer v;
        final int light;

        B(PoseStack pose, VertexConsumer v, int light) {
            this.m = pose.last().pose();
            this.n = pose.last().normal();
            this.v = v;
            this.light = light;
        }

        void vert(float x, float y, float z, int rgb, int a, int lt, Vector3f nm) {
            v.vertex(m, x, y, z).color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, a).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(lt).normal(n, nm.x, nm.y, nm.z).endVertex();
        }

        void quad(float[] a, float[] b2, float[] c, float[] d, int rgb, int alpha, int lt) {
            Vector3f u = new Vector3f(b2[0] - a[0], b2[1] - a[1], b2[2] - a[2]), w = new Vector3f(c[0] - a[0], c[1] - a[1], c[2] - a[2]);
            Vector3f nm = u.cross(w);
            if (nm.lengthSquared() < 1e-12f) nm.set(0, 1, 0);
            else nm.normalize();
            vert(a[0], a[1], a[2], rgb, alpha, lt, nm);
            vert(b2[0], b2[1], b2[2], rgb, alpha, lt, nm);
            vert(c[0], c[1], c[2], rgb, alpha, lt, nm);
            vert(d[0], d[1], d[2], rgb, alpha, lt, nm);
        }

        void box(float x1, float y1, float z1, float x2, float y2, float z2, int rgb) {
            boxLit(x1, y1, z1, x2, y2, z2, rgb, light);
        }

        void boxLit(float x1, float y1, float z1, float x2, float y2, float z2, int rgb, int lt) {
            float ax = Math.min(x1, x2), bx = Math.max(x1, x2), ay = Math.min(y1, y2), by = Math.max(y1, y2), az = Math.min(z1, z2), bz = Math.max(z1, z2);
            quad(new float[]{ax, by, az}, new float[]{ax, by, bz}, new float[]{bx, by, bz}, new float[]{bx, by, az}, rgb, 255, lt);
            quad(new float[]{ax, ay, bz}, new float[]{ax, ay, az}, new float[]{bx, ay, az}, new float[]{bx, ay, bz}, shade(rgb, 0.7f), 255, lt);
            quad(new float[]{ax, ay, bz}, new float[]{bx, ay, bz}, new float[]{bx, by, bz}, new float[]{ax, by, bz}, rgb, 255, lt);
            quad(new float[]{bx, ay, az}, new float[]{ax, ay, az}, new float[]{ax, by, az}, new float[]{bx, by, az}, rgb, 255, lt);
            quad(new float[]{bx, ay, bz}, new float[]{bx, ay, az}, new float[]{bx, by, az}, new float[]{bx, by, bz}, rgb, 255, lt);
            quad(new float[]{ax, ay, az}, new float[]{ax, ay, bz}, new float[]{ax, by, bz}, new float[]{ax, by, az}, rgb, 255, lt);
        }

        /**
         * A side profile (z, y), convex, extruded from x1 to x2. edge[i] colours the strip from point i to i+1:
         * -1 the body colour, -2 leave it open (glass goes there later).
         */
        void prism(float x1, float x2, float[][] p, int rgb, int[] edge) {
            prism(x1, x2, p, rgb, edge, true);
        }

        void prism(float x1, float x2, float[][] p, int rgb, int[] edge, boolean sides) {
            int nPts = p.length;
            for (int i = 1; sides && i + 1 < nPts; i++) {
                quad(new float[]{x2, p[0][1], p[0][0]}, new float[]{x2, p[i][1], p[i][0]}, new float[]{x2, p[i + 1][1], p[i + 1][0]}, new float[]{x2, p[i + 1][1], p[i + 1][0]}, rgb, 255, light);
                quad(new float[]{x1, p[0][1], p[0][0]}, new float[]{x1, p[i + 1][1], p[i + 1][0]}, new float[]{x1, p[i][1], p[i][0]}, new float[]{x1, p[i][1], p[i][0]}, rgb, 255, light);
            }
            for (int i = 0; i < nPts; i++) {
                int j = (i + 1) % nPts;
                int c = edge == null || i >= edge.length || edge[i] == -1 ? rgb : edge[i];
                if (c == -2) continue;
                // the top surfaces a little lighter, the bottom darker
                float dz = p[j][0] - p[i][0], dy = p[j][1] - p[i][1];
                int col = dy == 0 && dz > 0 ? shade(c, 0.6f) : c;
                quad(new float[]{x1, p[i][1], p[i][0]}, new float[]{x2, p[i][1], p[i][0]}, new float[]{x2, p[j][1], p[j][0]}, new float[]{x1, p[j][1], p[j][0]}, col, 255, light);
            }
        }

        /** A flat convex polygon (z, y) at x. */
        void poly(float x, float[][] p, int rgb) {
            for (int i = 1; i + 1 < p.length; i++)
                quad(new float[]{x, p[0][1], p[0][0]}, new float[]{x, p[i][1], p[i][0]}, new float[]{x, p[i + 1][1], p[i + 1][0]}, new float[]{x, p[i + 1][1], p[i + 1][0]}, rgb, 255, light);
        }

        /** A rectangle on a side (x fixed). */
        void side(float x, float y1, float z1, float y2, float z2, int rgb) {
            quad(new float[]{x, y1, z1}, new float[]{x, y1, z2}, new float[]{x, y2, z2}, new float[]{x, y2, z1}, rgb, 255, light);
        }

        /** A rectangle across the front or back (z fixed). */
        void across(float x1, float y1, float z, float x2, float y2, int rgb) {
            quad(new float[]{x1, y1, z}, new float[]{x2, y1, z}, new float[]{x2, y2, z}, new float[]{x1, y2, z}, rgb, 255, light);
        }

        /** A sheet of glass over the slope from profile point a to b, set out from the surface by d. */
        void slope(float x1, float x2, float[] a, float[] b2, float d, int rgb, int alpha) {
            float dz = b2[0] - a[0], dy = b2[1] - a[1], len = Mth.sqrt(dz * dz + dy * dy);
            float nz = dy / len, ny = -dz / len;   // outward for a profile drawn counter-clockwise
            float oz = nz * d, oy = ny * d;
            quad(new float[]{x1, a[1] + oy, a[0] + oz}, new float[]{x2, a[1] + oy, a[0] + oz}, new float[]{x2, b2[1] + oy, b2[0] + oz}, new float[]{x1, b2[1] + oy, b2[0] + oz}, rgb, alpha, light);
        }

        /** A side window clipped to the cabin profile between z1 and z2. */
        void sideWindow(float x, int s, float[][] cab, float z1, float z2, float yb, float yt, int rgb, int alpha) {
            // the window follows the windscreen slope at the front and the rear window slope at the back
            float t1 = topAt(cab, z1) - 0.05f, t2 = topAt(cab, z2) - 0.05f;
            quad(new float[]{x, yb, z1}, new float[]{x, yb, z2}, new float[]{x, Math.min(yt, t2), z2}, new float[]{x, Math.min(yt, t1), z1}, rgb, alpha, light);
        }

        private static float topAt(float[][] cab, float z) {
            // cab: rear bottom, front bottom, front top, rear top
            if (z > cab[2][0]) return cab[1][1] + (cab[2][1] - cab[1][1]) * (cab[1][0] - z) / (cab[1][0] - cab[2][0]);
            if (z < cab[3][0]) return cab[0][1] + (cab[3][1] - cab[0][1]) * (z - cab[0][0]) / (cab[3][0] - cab[0][0]);
            return cab[2][1];
        }

        /** A flat window on a side (x fixed) from (y1, z1) to (y2, z2). */
        void flat(float x, float y1, float z1, float y2, float z2, int s, int rgb, int alpha) {
            quad(new float[]{x, y1, z1}, new float[]{x, y1, z2}, new float[]{x, y2, z2}, new float[]{x, y2, z1}, rgb, alpha, light);
        }

        /** A flat window across the front or the back (z fixed). */
        void flat2(float x1, float y1, float z, float x2, float y2, int rgb, int alpha) {
            quad(new float[]{x1, y1, z}, new float[]{x2, y1, z}, new float[]{x2, y2, z}, new float[]{x1, y2, z}, rgb, alpha, light);
        }

        /** A disc facing along x (s = which way), centre (y, z). */
        void disc(float x, float y, float z, float r, int rgb, int seg, float phase, int s) {
            for (int i = 0; i < seg; i += 2) {
                float a0 = phase + i * Mth.TWO_PI / seg, a1 = phase + (i + 1) * Mth.TWO_PI / seg, a2 = phase + (i + 2) * Mth.TWO_PI / seg;
                quad(new float[]{x, y, z}, new float[]{x, y + r * Mth.sin(a0), z + r * Mth.cos(a0)}, new float[]{x, y + r * Mth.sin(a1), z + r * Mth.cos(a1)},
                        new float[]{x, y + r * Mth.sin(a2), z + r * Mth.cos(a2)}, rgb, 255, light);
            }
        }

        /** A spoke of a rim: a thin bar from radius r1 to r2 at angle a. */
        void spoke(float x, float y, float z, float r1, float r2, float a, float w, int rgb, int s) {
            float cy = Mth.sin(a), cz = Mth.cos(a), py = -cz * w / 2, pz = cy * w / 2;
            quad(new float[]{x, y + cy * r1 + py, z + cz * r1 + pz}, new float[]{x, y + cy * r2 + py, z + cz * r2 + pz},
                    new float[]{x, y + cy * r2 - py, z + cz * r2 - pz}, new float[]{x, y + cy * r1 - py, z + cz * r1 - pz}, rgb, 255, light);
        }

        /** A cylinder along x: the tread of a tyre. */
        void cylX(float x1, float x2, float y, float z, float r, int seg, int rgb, float phase) {
            for (int i = 0; i < seg; i++) {
                float a0 = phase + i * Mth.TWO_PI / seg, a1 = phase + (i + 1) * Mth.TWO_PI / seg;
                float y0 = y + r * Mth.sin(a0), z0 = z + r * Mth.cos(a0), y1 = y + r * Mth.sin(a1), z1 = z + r * Mth.cos(a1);
                quad(new float[]{x1, y0, z0}, new float[]{x2, y0, z0}, new float[]{x2, y1, z1}, new float[]{x1, y1, z1}, i % 2 == 0 ? rgb : shade(rgb, 1.25f), 255, light);
            }
            disc(x1, y, z, r, rgb, seg, phase, -1);
            disc(x2, y, z, r, rgb, seg, phase, 1);
        }
    }
}
