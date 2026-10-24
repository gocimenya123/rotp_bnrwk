package com.github.yourname.rotp_bnrwk.client.render;

import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import com.github.yourname.rotp_bnrwk.BnrwkAddon;
import com.github.yourname.rotp_bnrwk.entity.stand.BnrwkEntity;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class BnrwkRenderer extends StandEntityRenderer<BnrwkEntity, BnrwkModel> {

    public BnrwkRenderer(EntityRendererManager renderManager) {
        super(renderManager, new BnrwkModel(),
                new ResourceLocation(BnrwkAddon.MOD_ID, "textures/entity/stand/bnrwk.png"), 0);
    }
}
