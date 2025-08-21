package com.voltaicgrid.chatplaysmc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;

public class GrapplingHookEntity extends ProjectileEntity {
    @Nullable private String ownerName;
    private int life;
    private static final int MAX_LIFE = 200; // 10s at 20 tps

    // Hook state
    private boolean hooked;
    private Vec3d anchor;
    private int hookedTicks;
    
    // Store the orientation when the hook hits a surface (so it doesn't keep rotating)
    private float impactYaw;
    private float impactPitch;
    private boolean hasImpactOrientation = false;

    private boolean hasImpactEmbed;
    private Direction impactFace;        // block face hit
    private float impactOffsetX, impactOffsetY, impactOffsetZ; // tiny offset along face normal

    private static final double GRAVITY = 0.05; // light arc
    private static final double AIR_DRAG = 0.99;
    private static final double GROUND_DRAG = 0.6;
    private static final double PULL_STRENGTH = 0.4; // per-tick acceleration toward anchor

    // Tracked owner ID for client-side rendering
    private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(GrapplingHookEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    // Match EntityFactory<GrapplingHookEntity> signature to satisfy EntityType.Builder.create
    public GrapplingHookEntity(EntityType<GrapplingHookEntity> type, World world) {
        super(type, world);
        this.noClip = false;
    }

    public GrapplingHookEntity(World world, PlayerEntity owner) {
        this(ModEntities.GRAPPLING_HOOK, world);
        setOwner(owner);
    }

    public void setOwner(LivingEntity owner) {
        this.ownerName = owner.getName().getString();
        // Sync the owner entity id to clients for rope rendering
        this.getDataTracker().set(OWNER_ID, owner.getId());
    }

    public int getOwnerEntityId() {
        return this.getDataTracker().get(OWNER_ID);
    }
    
    // Methods to access the fixed impact orientation
    public boolean isHooked() {
        return this.hooked;
    }
    
    public boolean hasImpactOrientation() {
        return this.hasImpactOrientation;
    }
    
    public float getImpactYaw() {
        return this.impactYaw;
    }
    
    public float getImpactPitch() {
        return this.impactPitch;
    }
    
    public boolean hasImpactEmbed() { return hasImpactEmbed; }
    public float getImpactOffsetX() { return impactOffsetX; }
    public float getImpactOffsetY() { return impactOffsetY; }
    public float getImpactOffsetZ() { return impactOffsetZ; }

    public Direction getImpactFace() { return impactFace; } // optional, if you want it for debugging/UI

    @Nullable
    public PlayerEntity getOwner() {
        if (ownerName == null) return null;
        // Server-side lookup by name (Client-side return null to avoid dependency on client classes here)
        if (this.getWorld() != null && this.getWorld().getServer() != null) {
            return this.getWorld().getServer().getPlayerManager().getPlayer(ownerName);
        }
        return null;
    }

    @Override
    public void initDataTracker(DataTracker.Builder builder) {
        // Initialize any data tracker entries if needed
        // For a basic grappling hook, we might not need any tracked data
        builder.add(OWNER_ID, -1);
    }
    
    @Override
    public void tick() {
        super.tick();
        life++;
        if (!this.getWorld().isClient && life > MAX_LIFE) {
            this.discard();
            return;
        }

        // --- A. Run the same basic physics on BOTH sides for smoothness ---
        // (Server is still authoritative; client just predicts visuals.)
        if (!hooked) {
            // Apply gravity
            Vec3d vel = this.getVelocity().add(0.0, -GRAVITY, 0.0);

            // Use sub-steps for both sides (server & client) to keep paths consistent
            final double maxStep = 0.25; // max distance per sub-step
            final double dist = vel.length();
            int steps = Math.max(1, (int)Math.ceil(dist / maxStep));
            Vec3d stepVel = vel.multiply(1.0 / steps);

            for (int i = 0; i < steps; i++) {
                Vec3d start = this.getPos();
                Vec3d end   = start.add(stepVel);

                HitResult hit = this.getWorld().raycast(new RaycastContext(
                    start, end,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    this
                ));

                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult bhr = (BlockHitResult) hit;

                    // 1) Anchor (exact hit point)
                    this.anchor = hit.getPos();
                    this.hooked = true;
                    this.hookedTicks = 0;

                    // 2) Freeze motion & collisions
                    this.setPosition(anchor.x, anchor.y, anchor.z);
                    this.setVelocity(Vec3d.ZERO);
                    this.noClip = true;

                    // 3) Freeze orientation ONCE (use current entity orientation or compute from flight vector)
                    if (!this.hasImpactOrientation) {
                        // Option A: use current yaw/pitch (simple & usually good enough)
                        this.impactYaw = this.getYaw();
                        this.impactPitch = this.getPitch();

                        // Option B: compute from the projectile's last motion vector for true "flight-facing":
                        // Vec3d v = this.getVelocity().normalize();
                        // double lenXZ = Math.sqrt(v.x*v.x + v.z*v.z);
                        // this.impactYaw   = (float)Math.toDegrees(Math.atan2(v.x, v.z));
                        // this.impactPitch = (float)-Math.toDegrees(Math.atan2(v.y, lenXZ));

                        this.hasImpactOrientation = true;
                    }

                    // 4) Record the block face and a *fixed* embed offset so it never changes later
                    if (!this.hasImpactEmbed) {
                        this.impactFace = bhr.getSide();
                        // Nudge out of the block along the face normal to prevent z-fighting and “sinking”
                        // ~1/32 to 1/16 of a block is typical. Tune as needed.
                        float nudge = 1.0f / 32.0f;

                        Vec3i n = this.impactFace.getVector(); // (nx, ny, nz) in [-1,0,1]
                        this.impactOffsetX = nudge * n.getX();
                        this.impactOffsetY = nudge * n.getY();
                        this.impactOffsetZ = nudge * n.getZ();

                        this.hasImpactEmbed = true;
                    }
                } else {
                    // Move a small step
                    this.move(MovementType.SELF, stepVel);
                }
            }

            // Drag after all sub-steps
            double drag = this.isOnGround() ? GROUND_DRAG : AIR_DRAG;
            this.setVelocity(this.getVelocity().add(0.0, -GRAVITY, 0.0).multiply(drag));

            // --- B. Server-only bits below ---
            if (this.getWorld().isClient) {
                // Client is done: predicted move already applied above.
                return;
            }

            // Mark velocity for network sync so clients get smoother interpolation
            // (setVelocity already marks it dirty in modern mappings, but this helps readability)
            this.velocityDirty = true; // if accessible; otherwise rely on setVelocity(...)
        } else {
            // Hooked: keep at anchor (both sides so it doesn’t jitter client-side)
        	if (this.hooked && this.anchor != null) {
        	    this.setPosition(anchor.x, anchor.y, anchor.z);
        	    this.setVelocity(Vec3d.ZERO);
        	}

            hookedTicks++;

            // Server-only: pulling logic
            if (!this.getWorld().isClient) {
                PlayerEntity owner = getOwner();
                if (owner == null || !owner.isAlive()) {
                    this.discard();
                    return;
                }

                Vec3d ownerPos = owner.getPos().add(0, owner.getStandingEyeHeight() * 0.5, 0);
                Vec3d toAnchor = anchor.subtract(ownerPos);
                double distance = toAnchor.length();

                if (distance < 1.3 || hookedTicks > MAX_LIFE) {
                    this.discard();
                    return;
                }

                Vec3d dir = distance > 0 ? toAnchor.normalize() : Vec3d.ZERO;
                double strength = Math.min(PULL_STRENGTH, Math.max(0.08, distance * 0.05));

                Vec3d newVel = owner.getVelocity().multiply(0.90).add(dir.multiply(strength));
                owner.setVelocity(newVel);
                owner.fallDistance = 0.0F;
                owner.velocityModified = true;
            }
        }
    }

    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.life = nbt.getInt("Life").orElse(0);
        if (nbt.contains("Owner")) this.ownerName = nbt.getString("Owner").orElse(null);
        this.hooked = nbt.getBoolean("Hooked").orElse(false);
        if (this.hooked && nbt.contains("AnchorX")) {
            this.anchor = new Vec3d(nbt.getDouble("AnchorX").orElse(0.0), nbt.getDouble("AnchorY").orElse(0.0), nbt.getDouble("AnchorZ").orElse(0.0));
        }

        this.hasImpactOrientation = nbt.getBoolean("HasImpactOrientation").orElse(false);
        if (this.hasImpactOrientation) {
            this.impactYaw   = nbt.getFloat("ImpactYaw").orElse(0.0f);
            this.impactPitch = nbt.getFloat("ImpactPitch").orElse(0.0f);
        }

        this.hasImpactEmbed = nbt.getBoolean("HasImpactEmbed").orElse(false);
        if (this.hasImpactEmbed) {
            this.impactOffsetX = nbt.getFloat("ImpactOffsetX").orElse(0.0f);
            this.impactOffsetY = nbt.getFloat("ImpactOffsetY").orElse(0.0f);
            this.impactOffsetZ = nbt.getFloat("ImpactOffsetZ").orElse(0.0f);
            if (nbt.contains("ImpactFace")) {
                try {     
                	this.impactFace = Direction.byId(nbt.getString("ImpactFace").orElse(null)); 
            	} catch (Exception ignored) {}
            }
        }
    }

    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Life", this.life);
        if (ownerName != null) nbt.putString("Owner", ownerName);
        nbt.putBoolean("Hooked", this.hooked);
        if (this.hooked && this.anchor != null) {
            nbt.putDouble("AnchorX", this.anchor.x);
            nbt.putDouble("AnchorY", this.anchor.y);
            nbt.putDouble("AnchorZ", this.anchor.z);
        }

        nbt.putBoolean("HasImpactOrientation", this.hasImpactOrientation);
        if (this.hasImpactOrientation) {
            nbt.putFloat("ImpactYaw", this.impactYaw);
            nbt.putFloat("ImpactPitch", this.impactPitch);
        }

        nbt.putBoolean("HasImpactEmbed", this.hasImpactEmbed);
        if (this.hasImpactEmbed) {
            nbt.putFloat("ImpactOffsetX", this.impactOffsetX);
            nbt.putFloat("ImpactOffsetY", this.impactOffsetY);
            nbt.putFloat("ImpactOffsetZ", this.impactOffsetZ);
            if (this.impactFace != null) nbt.putString("ImpactFace", this.impactFace.asString());
        }
    }
}
