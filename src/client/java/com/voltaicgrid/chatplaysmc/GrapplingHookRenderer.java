package com.voltaicgrid.chatplaysmc;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.LightmapTextureManager;

/**
 * Renderer for the grappling hook entity. Renders a 3D grappling hook model and
 * a rope back to the owning player.
 */
public class GrapplingHookRenderer extends EntityRenderer<GrapplingHookEntity, GrapplingHookEntityRenderState> {
	private static final Identifier GRAPPLING_HOOK_TEXTURE = Identifier.of("chat_plays_mc",
			"textures/entity/grappling_hook.png");
	private final GrapplingHookEntityModel model;

	public GrapplingHookRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new GrapplingHookEntityModel(ctx.getPart(GrapplingHookEntityModel.LAYER_LOCATION));
	}

	@Override
	public GrapplingHookEntityRenderState createRenderState() {
		return new GrapplingHookEntityRenderState();
	}

	@Override
	public void updateRenderState(GrapplingHookEntity entity,
	                              GrapplingHookEntityRenderState state,
	                              float tickDelta) {
	    super.updateRenderState(entity, state, tickDelta);

	    state.age   = entity.age + tickDelta;
	    state.pitch = entity.getPitch();
	    state.yaw   = entity.getYaw();

	    // Rope vector: keep your existing code if needed; omitted here for brevity
	    // state.hasRope, ropeDx/Dy/Dz, etc.

	    // --- Orientation ---
	    if (entity.isHooked() && entity.hasImpactOrientation()) {
	        state.facePlayerYaw   = entity.getImpactYaw();
	        state.facePlayerPitch = entity.getImpactPitch();
	    } else {
	        // Flying: face along velocity (stable), with MC-style yaw/pitch
	        Vec3d v = entity.getVelocity();
	        if (v.lengthSquared() > 1.0e-6) {
	            double lenXZ = Math.sqrt(v.x*v.x + v.z*v.z);
	            state.facePlayerYaw   = (float) Math.toDegrees(Math.atan2(v.x, v.z));
	            state.facePlayerPitch = (float)-Math.toDegrees(Math.atan2(v.y, lenXZ));
	        } else {
	            state.facePlayerYaw   = state.yaw;
	            state.facePlayerPitch = state.pitch;
	        }
	    }

	    // --- Embed offset (frozen) ---
	    if (entity.isHooked() && entity.hasImpactEmbed()) {
	        state.embedOffsetX = entity.getImpactOffsetX();
	        state.embedOffsetY = entity.getImpactOffsetY();
	        state.embedOffsetZ = entity.getImpactOffsetZ();
	    } else {
	        state.embedOffsetX = state.embedOffsetY = state.embedOffsetZ = 0f;
	    }
	}

	// Draw a thin rope from the hook's origin (0,0,0) toward the vector stored in
	// render state.
	@Override
	public void render(GrapplingHookEntityRenderState state, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		
	    if ((state.age % 5f) < 0.1f) System.out.println("Hook render tick: " + state.age);

		if (state.hasRope) {
			matrices.push();
			VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getLeash());
			MatrixStack.Entry entry = matrices.peek();

			double dx = state.ropeDx;
			double dy = state.ropeDy;
			double dz = state.ropeDz;
			int segments = 20;

			// Make it easy to see while debugging: fullbright and thicker
			int ropeLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
			int r = 200, g = 180, b = 140; // rope-ish color
			float halfWidth = 0.04f;

			// Curve with mild sag
			java.util.function.DoubleFunction<Vec3d> curve = (double tt) -> {
				double x = dx * tt;
				double z = dz * tt;
				double y = dy * (tt * tt + tt) * 0.5
						- Math.sin(Math.PI * tt) * Math.min(0.4, Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.02);
				return new Vec3d(x, y, z);
			};

			// Initialize first point and side vector
			Vec3d p0 = curve.apply(0.0);
			Vec3d p1 = curve.apply(1.0 / segments);
			Vec3d dir = p1.subtract(p0);
			Vec3d up = new Vec3d(0, 1, 0);
			Vec3d side = dir.crossProduct(up);
			if (side.lengthSquared() < 1.0e-6)
				side = dir.crossProduct(new Vec3d(1, 0, 0));
			side = side.normalize().multiply(halfWidth);

			// Seed the strip with the first two vertices
			Vec3d p0L = p0.add(side);
			Vec3d p0R = p0.subtract(side);
			vc.vertex(entry, (float) p0L.x, (float) p0L.y, (float) p0L.z).color(r, g, b, 255).light(ropeLight)
					.normal(entry, 0.0f, 1.0f, 0.0f);
			vc.vertex(entry, (float) p0R.x, (float) p0R.y, (float) p0R.z).color(r, g, b, 255).light(ropeLight)
					.normal(entry, 0.0f, 1.0f, 0.0f);

			// Build triangle strip: for each segment emit the next left/right pair
			for (int i = 1; i <= segments; i++) {
				double t = i / (double) segments;
				Vec3d p = curve.apply(t);
				// Recompute side using local tangent to keep ribbon facing roughly sideways
				Vec3d prev = curve.apply(Math.max(0.0, t - 1.0 / segments));
				Vec3d tangent = p.subtract(prev);
				Vec3d s = tangent.crossProduct(up);
				if (s.lengthSquared() < 1.0e-6)
					s = tangent.crossProduct(new Vec3d(1, 0, 0));
				s = s.normalize().multiply(halfWidth);

				Vec3d left = p.add(s);
				Vec3d right = p.subtract(s);

				vc.vertex(entry, (float) left.x, (float) left.y, (float) left.z).color(r, g, b, 255).light(ropeLight)
						.normal(entry, 0.0f, 1.0f, 0.0f);
				vc.vertex(entry, (float) right.x, (float) right.y, (float) right.z).color(r, g, b, 255).light(ropeLight)
						.normal(entry, 0.0f, 1.0f, 0.0f);
			}
			matrices.pop();
		}

		matrices.push();

		// Because embed offsets are world-space along the hit face normal, apply them first.
		matrices.translate(state.embedOffsetX, state.embedOffsetY, state.embedOffsetZ);

		// Then rotate
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.facePlayerYaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(state.facePlayerPitch));

		// draw model...
		matrices.pop();
	}

	// No @Override to avoid signature mismatch across mappings
	public Identifier getTexture(GrapplingHookEntity entity) {
		return GRAPPLING_HOOK_TEXTURE;
	}
}
