package com.github.yourname.rotp_bnrwk.client.render;

import com.github.yourname.rotp_bnrwk.BnrwkAddon;
import com.github.yourname.rotp_bnrwk.entity.ClockBombEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.util.math.vector.Vector3f;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;

/** Draws the clock pin oriented along the face it is planted in; flying pins tumble. */
public class ClockBombRenderer extends EntityRenderer<ClockBombEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(BnrwkAddon.MOD_ID, "textures/entity/clock_bomb.png");
    private final ClockBombModel model = new ClockBombModel();

    public ClockBombRenderer(EntityRendererManager renderManager) {
        super(renderManager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ClockBombEntity bomb, float yaw, float partialTick, MatrixStack ms,
                       IRenderTypeBuffer buffer, int light) {
        ms.pushPose();
        ms.translate(0.0D, bomb.getBbHeight() * 0.5D, 0.0D);

        // Orient so that local "up" = outward normal of the surface the pin is in.
        Direction face = bomb.getFacing();
        switch (face) {
        case DOWN:  ms.mulPose(Vector3f.XP.rotationDegrees(180.0F)); break;
        case NORTH: ms.mulPose(Vector3f.XP.rotationDegrees(-90.0F)); break;
        case SOUTH: ms.mulPose(Vector3f.XP.rotationDegrees(90.0F));  break;
        case EAST:  ms.mulPose(Vector3f.ZP.rotationDegrees(-90.0F)); break;
        case WEST:  ms.mulPose(Vector3f.ZP.rotationDegrees(90.0F));  break;
        default: break;
        }

        float time = bomb.tickCount + partialTick;
        if (bomb.getState() == ClockBombEntity.STATE_FLYING) {
            ms.mulPose(Vector3f.YP.rotationDegrees(time * 40.0F));
            ms.mulPose(Vector3f.XP.rotationDegrees(time * 25.0F));
        }

        float scale = bomb.isSmall() ? 0.6F : 1.0F;
        ms.scale(-scale, -scale, scale);   // model space has +Y pointing down
        model.setBombPose(bomb.getSpringProgress(partialTick), time);
        IVertexBuilder vb = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(ms, vb, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        ms.popPose();
        super.render(bomb, yaw, partialTick, ms, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ClockBombEntity bomb) {
        return TEXTURE;
    }
}
