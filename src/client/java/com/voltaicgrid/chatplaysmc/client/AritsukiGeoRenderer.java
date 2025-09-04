package com.voltaicgrid.chatplaysmc.client;

import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.voltaicgrid.chatplaysmc.AritsukiEntity;
import com.voltaicgrid.chatplaysmc.ChatPlaysMcMod;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.layer.ItemInHandGeoLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

public class AritsukiGeoRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<AritsukiEntity, R> {

    public AritsukiGeoRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new AritsukiGeoModel());
        addRenderLayer(new ItemInHandGeoLayer(this));
        this.shadowRadius = 0.5f;
    }

//     public Identifier getTexture(LivingEntityRenderState state) {
//         return Identifier.of(ChatPlaysMcMod.MOD_ID, "textures/entity/aritsuki");
//     }
}