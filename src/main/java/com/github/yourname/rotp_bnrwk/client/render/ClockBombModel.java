package com.github.yourname.rotp_bnrwk.client.render;

import com.github.yourname.rotp_bnrwk.entity.ClockBombEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;

/**
 * Clock-shaped pin. Local -Y is "outwards" from the surface it is planted in.
 *   - spring = 0: the clock face lies flush on the surface, the pin shaft is buried (hidden)
 *   - spring = 1: the shaft has shot out and pushed the clock face 5px away from the surface
 * The minute hand spins faster the closer the pin is to exploding.
 */
public class ClockBombModel extends EntityModel<ClockBombEntity> {
    private final ModelRenderer pin;
    private final ModelRenderer face;
    private final ModelRenderer hand;

    public ClockBombModel() {
        super(RenderType::entityCutoutNoCull);
        texWidth = 32;
        texHeight = 32;

        pin = new ModelRenderer(this);
        pin.setPos(0F, 0F, 0F);
        pin.texOffs(0, 8).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F, 0F);

        face = new ModelRenderer(this);
        face.setPos(0F, 0F, 0F);
        face.texOffs(0, 0).addBox(-3F, -1F, -3F, 6F, 1F, 6F, 0F);

        hand = new ModelRenderer(this);
        hand.setPos(0F, -1F, 0F);
        hand.texOffs(0, 16).addBox(-0.5F, -0.3F, -2.5F, 1F, 0.3F, 3F, 0F);
        face.addChild(hand);
    }

    /** @param spring 0..1 spring-up progress, @param time ticks (with partial) for the hand animation */
    public void setBombPose(float spring, float time) {
        pin.visible = spring > 0.02F;
        pin.y = (1F - spring) * 5F;
        face.y = -5F * spring;
        hand.yRot = time * (0.15F + 0.6F * spring);
    }

    @Override
    public void setupAnim(ClockBombEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {}

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        pin.render(matrixStack, buffer, light, overlay, red, green, blue, alpha);
        face.render(matrixStack, buffer, light, overlay, red, green, blue, alpha);
    }
}
