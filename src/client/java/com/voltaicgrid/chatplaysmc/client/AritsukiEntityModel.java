package com.voltaicgrid.chatplaysmc.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

import com.voltaicgrid.chatplaysmc.AritsukiEntity;


public class AritsukiEntityModel extends EntityModel<EntityRenderState> {
    private final ModelPart root;
    private final ModelPart HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG;

    public AritsukiEntityModel(ModelPart root) {
        super(root);             // REQUIRED in 1.21.x
        this.root = root;
        this.HEAD = root.getChild("HEAD");
        this.BODY = root.getChild("BODY");
        this.RIGHT_ARM = root.getChild("RIGHT_ARM");
        this.LEFT_ARM = root.getChild("LEFT_ARM");
        this.RIGHT_LEG = root.getChild("RIGHT_LEG");
        this.LEFT_LEG = root.getChild("LEFT_LEG");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot();

        r.addChild("HEAD",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(-4, -8, -4, 8, 8, 8, new Dilation(0.0F))
                .uv(32, 0).cuboid(-4, -8, -4, 8, 8, 8, new Dilation(0.5F)),
            ModelTransform.of(0, 0, 0, 0, 0, 0));

        r.addChild("BODY",
            ModelPartBuilder.create()
                .uv(16, 16).cuboid(-4, 0, -2, 8, 12, 4, new Dilation(0.0F))
                .uv(16, 32).cuboid(-4, 0, -2, 8, 12, 4, new Dilation(0.25F)),
            ModelTransform.of(0, 0, 0, 0, 0, 0));

        r.addChild("RIGHT_ARM",
            ModelPartBuilder.create()
                .uv(40, 16).cuboid(-2, -2, -2, 3, 12, 4, new Dilation(0.0F))
                .uv(40, 32).cuboid(-2, -2, -2, 3, 12, 4, new Dilation(0.25F)),
            ModelTransform.of(-5, 2, 0, 0, 0, 0));

        r.addChild("LEFT_ARM",
            ModelPartBuilder.create()
                .uv(32, 48).cuboid(-1, -2, -2, 3, 12, 4, new Dilation(0.0F))
                .uv(48, 48).cuboid(-1, -2, -2, 3, 12, 4, new Dilation(0.25F)),
            ModelTransform.of(5, 2, 0, 0, 0, 0));

        r.addChild("RIGHT_LEG",
            ModelPartBuilder.create()
                .uv(0, 16).cuboid(-2, 0, -2, 4, 12, 4, new Dilation(0.0F))
                .uv(0, 32).cuboid(-2, 0, -2, 4, 12, 4, new Dilation(0.25F)),
            ModelTransform.of(-1.9F, 12, 0, 0, 0, 0));

        r.addChild("LEFT_LEG",
            ModelPartBuilder.create()
                .uv(16, 48).cuboid(-2, 0, -2, 4, 12, 4, new Dilation(0.0F))
                .uv(0, 48).cuboid(-2, 0, -2, 4, 12, 4, new Dilation(0.25F)),
            ModelTransform.of(1.9F, 12, 0, 0, 0, 0));

        return TexturedModelData.of(md, 64, 64);
    }

    @Override
    public void setAngles(EntityRenderState state) {
        // animate using fields from `state` if you wish
    }
}
