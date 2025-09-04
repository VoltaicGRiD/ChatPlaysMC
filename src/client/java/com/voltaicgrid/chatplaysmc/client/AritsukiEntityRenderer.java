package com.voltaicgrid.chatplaysmc.client;

import com.voltaicgrid.chatplaysmc.AritsukiEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class AritsukiEntityRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<AritsukiEntity, R> {
	
	public AritsukiEntityRenderer(EntityRendererFactory.Context ctx) {
	    super(ctx, new AritsukiGeoModel());
	    this.shadowRadius = 0.5f;
	}
}