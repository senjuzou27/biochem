package ru.ufamod.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.ufamod.UfaMod;
import ru.ufamod.entity.NpcEntity;
import ru.ufamod.entity.NpcRole;

import java.util.HashMap;
import java.util.Map;

/** NPCs are drawn with the ordinary player model and one 64x64 skin per role / variant. */
public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, PlayerModel<NpcEntity>> {
    private static final Map<String, ResourceLocation> SKINS = new HashMap<>();

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    /** Enemy soldiers stay faintly visible at night, as if lit from afar, without glowing. */
    @Override
    protected int getBlockLightLevel(NpcEntity entity, net.minecraft.core.BlockPos pos) {
        int light = super.getBlockLightLevel(entity, pos);
        return entity.getRole() == NpcRole.ENEMY ? Math.max(light, 8) : light;
    }

    /** The wounded soldier lies face down on the ground, the way a swimming player is drawn. */
    @Override
    protected void setupRotations(NpcEntity entity, com.mojang.blaze3d.vertex.PoseStack pose, float age, float yaw, float partial) {
        super.setupRotations(entity, pose, age, yaw, partial);
        if (entity.getRole() == NpcRole.WOUNDED) {
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90f));
            pose.translate(0f, -1.0f, 0.3f);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        NpcRole role = entity.getRole();
        String key = role.skin + "_" + Math.floorMod(entity.getVariant(), role.variants);
        return SKINS.computeIfAbsent(key, k -> ResourceLocation.fromNamespaceAndPath(UfaMod.MODID, "textures/entity/npc/" + k + ".png"));
    }
}
