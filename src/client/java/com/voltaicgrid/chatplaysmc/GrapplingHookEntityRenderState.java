package com.voltaicgrid.chatplaysmc;

import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Render state for the grappling hook entity.
 * Contains the data needed to render the grappling hook.
 */
public class GrapplingHookEntityRenderState extends EntityRenderState {
    public float age;
    public float pitch;
    public float yaw;

    // Rope data: vector from hook to owner's eye (in world space), packed for renderer
    public boolean hasRope;
    public double ropeDx;
    public double ropeDy;
    public double ropeDz;
    
    // Player-facing rotation angles (for model orientation)
    public float facePlayerYaw;
    public float facePlayerPitch;
    
    // Embedding offset to make the hook appear stuck into the surface
    public float embedOffsetX;
    public float embedOffsetY;
    public float embedOffsetZ;
    
    private boolean hasImpactOrientation;
    private float impactYaw, impactPitch;

    private boolean hasImpactEmbed;
    private Vec3d anchor;                // you already have this
    private Direction impactFace;        // block face hit
    private float impactOffsetX, impactOffsetY, impactOffsetZ; // tiny offset along face normal
}