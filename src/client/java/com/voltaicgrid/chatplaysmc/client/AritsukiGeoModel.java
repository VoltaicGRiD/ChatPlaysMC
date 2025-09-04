package com.voltaicgrid.chatplaysmc.client;

import com.voltaicgrid.chatplaysmc.AritsukiEntity;
import com.voltaicgrid.chatplaysmc.ChatPlaysMcMod;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class AritsukiGeoModel extends GeoModel<AritsukiEntity> {

    // NOTE: GeoModel in the current GeckoLib version expects methods with the animatable type,
    // not GeoRenderState. The previous state-based methods were not overriding anything and
    // caused a compilation error because getAnimationResource(AritsukiEntity) was still abstract.

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        // Placeholder texture (uses vanilla Steve until a custom one is supplied)
        return Identifier.of(ChatPlaysMcMod.MOD_ID, "textures/entity/aritsuki.png");
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        // Full relative path INCLUDING folder and extension for GeckoLib 5.x
        return Identifier.of(ChatPlaysMcMod.MOD_ID, "aritsuki");
    }

    @Override
    public Identifier getAnimationResource(AritsukiEntity animatable) {
        // Uses existing animation file location (currently under geckolib/animations). Adjust later if moved.
        return Identifier.of(ChatPlaysMcMod.MOD_ID, "aritsuki");
    }
}