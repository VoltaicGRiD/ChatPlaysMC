package com.voltaicgrid.chatplaysmc;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import java.util.function.Function;

/**
 * Model for the grappling hook entity.
 * Currently a simple hook-like shape that can be expanded with more detail.
 */
public class GrapplingHookEntityModel extends EntityModel<EntityRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final EntityModelLayer LAYER_LOCATION = new EntityModelLayer(Identifier.of("chat_plays_mc", "grappling_hook"), "main");
	private final ModelPart Hook;
	private final ModelPart Rope;
	private final ModelPart Handle;

	public GrapplingHookEntityModel(ModelPart root) {
		super(root, RenderLayer::getEntitySolid);
		this.Hook = root.getChild("Hook");
		this.Rope = root.getChild("Rope");
		this.Handle = root.getChild("Handle");
	}

	public static TexturedModelData createModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		// Hook
		ModelPartData Hook = root.addChild(
		    "Hook",
		    ModelPartBuilder.create()
		        .uv(0, 16).cuboid(-1.0F, -1.0F, 6.0F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F))
		        .uv(7, 20).cuboid(-1.0F, -3.0F, 5.0F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F))
		        .uv(21, 20).cuboid(-1.0F,  1.0F, 5.0F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F))
		        .uv(21, 24).cuboid(-1.0F,  3.0F, 4.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(28, 19).cuboid( 0.0F,  4.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(28, 16).cuboid(-1.0F, -5.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(21, 16).cuboid(-1.0F, -6.0F, 1.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
		        .uv(0,  24).cuboid( 0.0F,  5.0F, 1.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
		        .uv(14, 24).cuboid(-1.0F, -4.0F, 4.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(7,  16).cuboid( 1.0F, -1.0F, 5.0F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F))
		        .uv(14, 27).cuboid( 3.0F, -1.0F, 4.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F))
		        .uv(19, 27).cuboid(-4.0F, -1.0F, 4.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F))
		        .uv(0,  28).cuboid( 4.0F, -1.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(5,  28).cuboid(-5.0F,  0.0F, 3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(14, 16).cuboid( 5.0F, -1.0F, 1.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
		        .uv(14, 20).cuboid(-6.0F,  0.0F, 1.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
		        .uv(0,  20).cuboid(-3.0F, -1.0F, 5.0F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F)),
		    ModelTransform.of(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F)
		);

		// Rope
		ModelPartData Rope = root.addChild(
		    "Rope",
		    ModelPartBuilder.create()
		        .uv(7,  24).cuboid(-1.0F, -1.0F,  5.0F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F))
		        .uv(24, 27).cuboid(-2.0F, -1.0F,  4.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F))
		        .uv(28, 22).cuboid(-2.0F,  0.0F,  3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(29, 25).cuboid(-1.0F,  1.0F,  3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(29, 25).cuboid( 0.0F,  1.0F,  3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(29, 28).cuboid( 1.0F,  1.0F,  2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(0,  31).cuboid( 1.0F,  0.0F,  2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(31, 0).cuboid( 1.0F, -1.0F,  1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(31, 3).cuboid( 1.0F, -2.0F,  1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(31, 3).cuboid( 0.0F, -2.0F,  0.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(5,  31).cuboid(-1.0F, -2.0F,  0.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(31, 6).cuboid(-2.0F, -2.0F, -1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(31, 9).cuboid(-2.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(10, 31).cuboid(-2.0F,  0.0F, -2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(31, 12).cuboid(-2.0F,  1.0F, -2.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(31, 12).cuboid(-1.0F,  1.0F, -3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(31, 12).cuboid( 0.0F,  1.0F, -3.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(29, 28).cuboid( 1.0F,  1.0F, -4.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(0,  31).cuboid( 1.0F,  0.0F, -4.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(31, 0).cuboid( 1.0F, -1.0F, -5.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(15, 31).cuboid( 1.0F, -2.0F, -5.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(20, 31).cuboid( 0.0F, -2.0F, -6.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(20, 31).cuboid(-1.0F, -2.0F, -6.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(20, 31).cuboid(-2.0F, -2.0F, -7.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
		        .uv(25, 31).cuboid(-2.0F, -1.0F, -7.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)),
		    ModelTransform.of(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F)
		);

		// Handle
		ModelPartData Handle = root.addChild(
		    "Handle",
		    ModelPartBuilder.create()
		        .uv(0, 0).cuboid(-1.0F, -3.0F, -4.0F, 2.0F, 2.0F, 13.0F, new Dilation(0.0F)),
		    ModelTransform.of(0.0F, 18.0F, -4.0F, 0.0F, 0.0F, 0.0F)
		);

		return TexturedModelData.of(modelData, 64, 64);
	}

	public void setupAnim(EntityRenderState entityRenderState, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	public void renderToBuffer(MatrixStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Hook.render(poseStack, vertexConsumer, packedLight, packedOverlay);
		Rope.render(poseStack, vertexConsumer, packedLight, packedOverlay);
		Handle.render(poseStack, vertexConsumer, packedLight, packedOverlay);
	}
}