package com.voltaicgrid.chatplaysmc;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AritsukiEntity extends PathAwareEntity implements GeoEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(AritsukiEntity.class);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Animations (RawAnimation definitions are light-weight; keep static)
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack.strike");
    private static final RawAnimation HEAD_TRACK = RawAnimation.begin().thenLoop("head.track");
    private static final RawAnimation DASH_ATTACK = RawAnimation.begin().thenPlay("attack.dash");

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        // Basic walk/idle controller: plays walk loop when moving, otherwise lets default idle occur.
        controllers.add(new AnimationController<AritsukiEntity>("walk", state -> {
            if (state.isMoving()) return state.setAndContinue(WALK);
            return PlayState.STOP;
        }));
        // Combat controller: stays idle until an animation is triggered.
        AritsukiEntity self = this;
        controllers.add(new AnimationController<AritsukiEntity>("combat", state -> {
            if (!self.attackAnimating && state.controller().getCurrentRawAnimation() == null) {
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }).triggerableAnim("attack", ATTACK));

        controllers.add(new AnimationController<AritsukiEntity>("head_track", state -> {
            return state.setAndContinue(HEAD_TRACK);
        }));

        // Dash controller: only active while an animation is currently playing (trigger driven)
        controllers.add(new AnimationController<AritsukiEntity>("dash", state -> {
            if (state.controller().getCurrentRawAnimation() == null) {
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }).triggerableAnim("dash_attack", DASH_ATTACK));
    }


    // Boss bar (purple) showing health progress
    private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Aritsuki"), BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
    private final ServerBossBar summonBar = new ServerBossBar(Text.literal("Time until next summon"), BossBar.Color.BLUE, BossBar.Style.PROGRESS);

    // Data tracker for syncing mode between server and client
    private static final TrackedData<Integer> COMBAT_MODE = DataTracker.registerData(AritsukiEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public enum CombatMode { 
        MELEE(0), RANGED(1), SUMMON(2);
        
        private final int id;
        CombatMode(int id) { this.id = id; }
        public int getId() { return id; }
        
        public static CombatMode fromId(int id) {
            for (CombatMode mode : values()) {
                if (mode.id == id) return mode;
            }
            return MELEE; // default fallback
        }
    }
    
    // Timers for special attacks
    private int specialAttackCooldown = 0;
    private int specialRangedAttackCooldown = 0;
    
    // Tracks whether a ranged attack ring/sequence is currently scheduled or executing
    private boolean rangedSequenceInProgress = false;
    
    // Prevent rapid flapping between modes & track last switch for debugging
    private int modeSwitchCooldown = 0; // ticks remaining before another switch allowed
    private int lastModeSwitchGameTime = 0;
    private @Nullable String lastDamageMsgId = null;
    
    // Constants for ranged mode behavior
    private static final double RANGED_MIN_DISTANCE = 8.0;
    private static final double RANGED_MAX_DISTANCE = 16.0;

    // Summon timing (per-instance)
    private int summonCount = 1;                 // increases after each summon wave
    private int timeSinceLastSummon = 0;          // ticks since last summon wave
    private float summonTimeMultiplier = 2.0f;    // base multiplier for interval scaling

    // Delayed melee impact handling
    private static final int ATTACK_IMPACT_DELAY_TICKS = 10; // ~0.5s into 0.75s animation
    private int delayedAttackTicks = -1;
    private int delayedAttackTargetId = -1;
    private int attackRecoveryTicks = 0; // cooldown after impact before another wind-up
    private boolean attackAnimating = false; // tracks if animation currently playing
    
    // Dash attack state
    private boolean dashInProgress = false;
    private int dashCooldownTicks = 0; // cooldown between dash attacks

    // Standard ranged attack cooldown
    private int rangedAttackCooldown = 0; // basic single-shot cooldown used by performRangedAttack

    public AritsukiEntity(EntityType<? extends AritsukiEntity> type, World world) {
        super(type, world);
    }
    
    @Override
    protected void initGoals() {
        // Clear existing goals using the correct predicate-based clear method
        this.goalSelector.clear(goal -> true);
        this.targetSelector.clear(goal -> true);
        
        // Basic AI goals - adjust priorities so ranged and summon can interrupt melee
        this.goalSelector.add(1, new AritsukiRangedBehaviorGoal(this));
        this.goalSelector.add(2, new AritsukiSummonBehaviorGoal(this));
        this.goalSelector.add(3, new AritsukiMeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(9, new WanderAroundFarGoal(this, 1.0));
        
        // Targeting
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }
    
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        boolean result = super.damage(world, source, amount); // process vanilla first
        if (this.getWorld().isClient) { // should not happen here, but guard
            LOGGER.info("AritsukiEntity (client) received damage (server override path): {}", source.getType().msgId());
            return result;
        }
        if (!result) return false; // cancelled / no actual damage
        LOGGER.info("[DamageOverride] Server damage hook invoked: {} amount {}", source.getType().msgId(), amount);
        handleServerDamage(source);
        return true;
    }

    private void handleServerDamage(DamageSource source) {
        // Detailed tag membership to verify classification
        boolean isProj = source.isIn(DamageTypeTags.IS_PROJECTILE);
        boolean isExpl = source.isIn(DamageTypeTags.IS_EXPLOSION);
        boolean isFall = source.isIn(DamageTypeTags.IS_FALL);
        String attackerType = source.getAttacker() == null ? "null" : String.valueOf(source.getAttacker().getType());
        String sourceEntityType = source.getSource() == null ? "null" : String.valueOf(source.getSource().getType());
        LOGGER.info("[DamageDebug] type={}, proj={}, expl={}, fall={}, attacker={}, sourceEntity={}",
        source.getType().msgId(), isProj, isExpl, isFall, attackerType, sourceEntityType);

        // Update target on server
        Entity attacker = source.getAttacker();
        if (attacker instanceof LivingEntity livingAttacker && livingAttacker != this.getTarget()) {
            this.setTarget(livingAttacker);
        }

        if (isFall) { // ignore fall
            LOGGER.info("AritsukiEntity hit by fall damage - ignoring for mode switching");
            return;
        }

        if (modeSwitchCooldown > 0) {
            LOGGER.info("Mode switch on cooldown ({} ticks remaining) - ignoring damage type {}", modeSwitchCooldown, source.getType().msgId());
            return;
        }

        ItemStack attackedByItem = source.getWeaponStack() == null ? ItemStack.EMPTY : source.getWeaponStack();
        if (!attackedByItem.isEmpty()) {
            LOGGER.info("[DamageDebug] Attacked by item: {}", attackedByItem.getItem().getName().getString());

            this.setStackInHand(this.getActiveHand(), attackedByItem);
        }

        CombatMode classified = classifyModeFromDamage(source);
        lastDamageMsgId = source.getType().msgId();
        setMode(classified);
        modeSwitchCooldown = 20; // 1 second lockout
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(COMBAT_MODE, CombatMode.MELEE.getId());
    }
    
    @Override
    protected void writeCustomData(WriteView write) {
        super.writeCustomData(write);
        write.putInt("CombatMode", getCurrentMode().getId());
        write.putInt("SpecialAttackCooldown", specialAttackCooldown);
        write.putInt("RangedAttackCooldown", specialRangedAttackCooldown);
    }
    
    @Override
    protected void readCustomData(ReadView read) {
        super.readCustomData(read);
        setMode(CombatMode.fromId(read.getInt("CombatMode", 0)));
        specialAttackCooldown = read.getInt("SpecialAttackCooldown", 0);
        specialRangedAttackCooldown = read.getInt("RangedAttackCooldown", 0);
    }
        
    public CombatMode getCurrentMode() {
        return CombatMode.fromId(this.dataTracker.get(COMBAT_MODE));
    }
    
    private void setMode(CombatMode newMode) {
        CombatMode currentMode = getCurrentMode();
        if (currentMode != newMode) {
            LOGGER.info("AritsukiEntity switching from {} mode to {} mode", currentMode, newMode);
            this.dataTracker.set(COMBAT_MODE, newMode.getId());
            // Reset navigation when switching modes
            this.getNavigation().stop();
            lastModeSwitchGameTime = this.age;
            // If leaving ranged mode mid-sequence, perform emergency cleanup
            if (currentMode == CombatMode.RANGED && rangedSequenceInProgress && newMode != CombatMode.RANGED) {
                this.setNoGravity(false);
                rangedSequenceInProgress = false;
                LOGGER.debug("[RangedAttack] Mode switched away during sequence -> cleanup gravity & flag reset");
            }
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.getWorld().isClient) {
            // (Removed TEMP debug forced ranged trigger)
            // Handle delayed melee impact
            if (delayedAttackTicks >= 0) {
                delayedAttackTicks--;
                if (delayedAttackTicks == 0) {
                    if (this.getWorld() instanceof ServerWorld serverWorld) {
                        Entity maybeTarget = serverWorld.getEntityById(delayedAttackTargetId);
                        if (maybeTarget instanceof LivingEntity living && living.isAlive()) {
                            double distSq = this.squaredDistanceTo(living);
                            // Basic range gate (approx melee reach)
                            if (distSq < 9.0) { // 3 blocks squared
                                super.tryAttack(serverWorld, living); // apply damage now
                            }
                        }
                    }
                    delayedAttackTargetId = -1;
                    delayedAttackTicks = -1; // mark wind-up complete
                    // Start recovery window once impact occurs
                    attackRecoveryTicks = 10; // half second recovery (adjust)
                }
            }
            if (attackRecoveryTicks > 0) attackRecoveryTicks--;
            // End of animation window: ATTACK animation length ~15 ticks (0.75s). When exceeded and no delay active, clear animating flag.
            if (attackAnimating && delayedAttackTicks < 0 && attackRecoveryTicks == 0) {
                attackAnimating = false;
            }
            // Decrease cooldowns
            if (specialAttackCooldown > 0) specialAttackCooldown--;
            if (specialRangedAttackCooldown > 0) specialRangedAttackCooldown--;
            if (rangedAttackCooldown > 0) rangedAttackCooldown--; // decrement basic ranged shot cooldown
            if (modeSwitchCooldown > 0) modeSwitchCooldown--;
            if (dashCooldownTicks > 0) dashCooldownTicks--;

            // Summon timer progression (server only)
            timeSinceLastSummon++;
            int threshold = getNextSummonThresholdTicks();
            if (timeSinceLastSummon >= threshold) {
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    performSummonAttack(serverWorld, this.getTarget());
                }
                timeSinceLastSummon = 0;
            }
        }
        
        // Temporarily get all our custom data for debugging
        if (this.age % 100 == 0) { // Log every 5 seconds
            LOGGER.info("AritsukiEntity Status - Mode: {}, ModeId: {}, SpecialCD: {}, RangedCD: {}, ModeSwitchCD: {}, Target: {}, LastDamage: {}, LastSwitchAge: {}", 
                getCurrentMode(), this.dataTracker.get(COMBAT_MODE), specialAttackCooldown, specialRangedAttackCooldown, modeSwitchCooldown, this.getTarget() != null, lastDamageMsgId, lastModeSwitchGameTime);
        }

        // Sync boss bar health each tick (server side only)
        if (!this.getWorld().isClient) {
            float percent = this.getMaxHealth() <= 0 ? 0f : this.getHealth() / this.getMaxHealth();
            bossBar.setPercent(percent);
            // Summon progress bar
            int threshold = getNextSummonThresholdTicks();
            float summonPercent = threshold > 0 ? (float) timeSinceLastSummon / threshold : 0f;
            summonBar.setPercent(Math.min(1f, summonPercent));
        }

        // Optional dash attack logic (only if we have a live target & in MELEE mode)
        LivingEntity dashTarget = this.getTarget();
        if (dashTarget != null && dashTarget.isAlive()) {
            double distanceFromTarget = this.getPos().distanceTo(dashTarget.getPos());
            // Lightweight debug every 3 seconds to avoid spam
            if (this.age % 60 == 0) {
                LOGGER.debug("DashCheck targetDist={}", String.format("%.2f", distanceFromTarget));
            }
            if (getCurrentMode() == CombatMode.MELEE && distanceFromTarget > 10.0 && distanceFromTarget <= 12.0) {
                if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld && !dashInProgress && dashCooldownTicks == 0) {
                    this.performDashAttack(serverWorld, dashTarget);
                }
            }
        }

        // While a dash sequence is in progress, keep forcibly facing the current target to avoid snapping back
        if (dashInProgress) {
            LivingEntity t = this.getTarget();
            if (t != null && t.isAlive()) {
                faceTargetInstant(t);
                if (this.age % 5 == 0) {
                    double dx = t.getX() - this.getX();
                    double dz = t.getZ() - this.getZ();
                    LOGGER.trace("DashFacing yaw={} dx={} dz={}", String.format("%.1f", this.getYaw()), String.format("%.2f", dx), String.format("%.2f", dz));
                }
            }
        }
    }

    public void tickMovement() {
        // IMPORTANT: Always call super to allow navigation, goal + movement updates.
        // The previous empty override prevented pathfinding + goal movement from running,
        // which made it look like "the AI broke" after adding animation controllers.
        super.tickMovement();

        if (!this.getWorld().isClient) {
            // Place any server-side custom movement adjustments here (currently none)
        }
    }

    /* ===================== Boss Bar Handling ===================== */
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
        summonBar.addPlayer(player);
    }

    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
        summonBar.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable net.minecraft.text.Text name) {
        super.setCustomName(name);
        if (name != null) bossBar.setName(name);
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        bossBar.clearPlayers();
    summonBar.clearPlayers();
    }
    

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        // Prevent new attack if winding up, recovering, or already animating
        if (delayedAttackTicks >= 0 || attackRecoveryTicks > 0 || attackAnimating) {
            return false;
        }
        this.getNavigation().stop();
        this.triggerAnim("combat", "attack");
        attackAnimating = true;
        delayedAttackTicks = ATTACK_IMPACT_DELAY_TICKS;
        delayedAttackTargetId = target.getId();
        return true; // wind-up started
    }

    /** Determine desired combat mode from an incoming damage source, with fallback. */
    private CombatMode classifyModeFromDamage(DamageSource source) {
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            LOGGER.info("AritsukiEntity classified damage {} as PROJECTILE -> RANGED", source.getType().msgId());
            return CombatMode.RANGED;
        }
        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            LOGGER.info("AritsukiEntity classified damage {} as EXPLOSION -> SUMMON", source.getType().msgId());
            return CombatMode.SUMMON;
        }
        // Additional heuristics: if the immediate source is a Wind Charge we treat as explosion-like
        Entity src = source.getSource();
        if (src != null) {
            String typeStr = src.getType().toString();
            if (typeStr.contains("tnt") || typeStr.contains("creeper") || typeStr.contains("wind_charge")) {
                LOGGER.info("AritsukiEntity heuristically classified source entity {} as explosion -> SUMMON", typeStr);
                return CombatMode.SUMMON;
            }
            if (typeStr.contains("arrow") || typeStr.contains("trident") || typeStr.contains("projectile")) {
                LOGGER.info("AritsukiEntity heuristically classified source entity {} as projectile -> RANGED", typeStr);
                return CombatMode.RANGED;
            }
        }
        LOGGER.info("AritsukiEntity defaulting damage {} to MELEE", source.getType().msgId());
        return CombatMode.MELEE;
    }
    
    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.MAX_HEALTH, 500.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.28)
            .add(EntityAttributes.ATTACK_DAMAGE, 0.5)
            .add(EntityAttributes.ATTACK_KNOCKBACK, 0.5)
            .add(EntityAttributes.FOLLOW_RANGE, 32.0)
            .add(EntityAttributes.ARMOR, 6.0);
    }
    
    // Custom AI Goal for Melee attacks (like zombies)
    private static class AritsukiMeleeAttackGoal extends MeleeAttackGoal {
        private final AritsukiEntity aritsuki;
        private boolean lastCanStart = false;
    // Tracks the last mode switch age at which we already fired the wind ring (no longer used)
        
        public AritsukiMeleeAttackGoal(AritsukiEntity aritsuki, double speed, boolean pauseWhenMobIdle) {
            super(aritsuki, speed, pauseWhenMobIdle);
            this.aritsuki = aritsuki;
        }
        
        @Override
        public boolean canStart() {
            boolean canStart = aritsuki.getCurrentMode() == CombatMode.MELEE && super.canStart();
            if (canStart != lastCanStart) {
                LOGGER.info("MeleeAttackGoal canStart changed: {} (mode: {}, hasTarget: {})", 
                    canStart, aritsuki.getCurrentMode(), aritsuki.getTarget() != null);
                lastCanStart = canStart;
            }
            return canStart;
        }
        
        @Override
        public boolean shouldContinue() {
            return aritsuki.getCurrentMode() == CombatMode.MELEE && super.shouldContinue();
        }

        @Override
        public void start() {
            // Ensure base MeleeAttackGoal initialization (timers, path recalculation) executes
            super.start();
            // Only trigger the wind ring once per melee mode switch
            // if (!aritsuki.getWorld().isClient && aritsuki.getWorld() instanceof ServerWorld serverWorld) {
            //     // When the entity switches modes, AritsukiEntity#setMode stores the current age in lastModeSwitchGameTime.
            //     // Fire the wind ring if we haven't processed this switch age yet and we're actually in melee mode.
            //     if (aritsuki.getCurrentMode() == CombatMode.MELEE && aritsuki.lastModeSwitchGameTime != lastProcessedSwitchAge) {
            //         aritsuki.performWindRingAttack(serverWorld, 45);
            //         lastProcessedSwitchAge = aritsuki.lastModeSwitchGameTime;
            //     }
            // }
        }
    }
    
    // Custom AI Goal for Ranged behavior
    private static class AritsukiRangedBehaviorGoal extends Goal {
        private final AritsukiEntity aritsuki;
        private LivingEntity target;
        private int attackTime = -1;
        private boolean lastCanStart = false;
        
        public AritsukiRangedBehaviorGoal(AritsukiEntity aritsuki) {
            this.aritsuki = aritsuki;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            LivingEntity currentTarget = aritsuki.getTarget();
            CombatMode currentMode = aritsuki.getCurrentMode();
            boolean hasTarget = currentTarget != null;
            boolean targetAlive = currentTarget != null && currentTarget.isAlive();
            boolean canSeeTarget = currentTarget != null && aritsuki.canSee(currentTarget);
            
            boolean canStart = currentMode == CombatMode.RANGED && 
                               hasTarget && targetAlive && canSeeTarget;
            
            // Log more detailed information about why the goal can't start
            if (currentMode == CombatMode.RANGED && !canStart) {
                LOGGER.info("RangedBehaviorGoal NOT starting - mode: {}, hasTarget: {}, targetAlive: {}, canSee: {}", 
                    currentMode, hasTarget, targetAlive, canSeeTarget);
            }
            
            if (canStart != lastCanStart) {
                LOGGER.info("RangedBehaviorGoal canStart changed: {} (mode: {}, hasTarget: {}, canSee: {})", 
                    canStart, currentMode, hasTarget, canSeeTarget);
                lastCanStart = canStart;
            }
            return canStart;
        }
        
        @Override
        public void start() {
            this.target = aritsuki.getTarget();
            this.attackTime = 0;
        }
        
        @Override
        public void stop() {
            this.target = null;
            this.attackTime = -1;
        }
        
        @Override
        public boolean shouldContinue() {
            return canStart();
        }
        
        @Override
        public void tick() {
            if (target == null) return;
            
            double distSq = aritsuki.squaredDistanceTo(target);
            double dist = Math.sqrt(distSq);
            double minDistanceSq = RANGED_MIN_DISTANCE * RANGED_MIN_DISTANCE;
            double maxDistanceSq = RANGED_MAX_DISTANCE * RANGED_MAX_DISTANCE;
            
            // Look at target with vanilla-style smooth turn (similar to skeleton behavior)
            aritsuki.getLookControl().lookAt(target, 30.0F, 30.0F);
            
            // Movement behavior (compare squared distances)
            if (distSq < minDistanceSq) {
                // Too close, back away - calculate position away from target
                Vec3d direction = aritsuki.getPos().subtract(target.getPos()).normalize();
                Vec3d backAwayPos = aritsuki.getPos().add(direction.multiply(5.0));
                aritsuki.getNavigation().startMovingTo(backAwayPos.x, backAwayPos.y, backAwayPos.z, 1.0);
            } else if (distSq > maxDistanceSq) {
                // Too far, get closer
                aritsuki.getNavigation().startMovingTo(target, 1.0);
            } else {
                // In good range, stop moving and prepare to attack
                aritsuki.getNavigation().stop();
            }
            
            // Attack logic (placeholder wired to stub method)
            this.attackTime++;
            if (this.attackTime >= 30) { // 3s cadence @20tps
                if (!aritsuki.getWorld().isClient && aritsuki.getWorld() instanceof ServerWorld serverWorld) {
                    boolean didAttack = false;
                    if (dist > 15.0 && aritsuki.specialRangedAttackCooldown <= 0 && !aritsuki.rangedSequenceInProgress) {
                        // Use special ranged attack when far and off cooldown
                        aritsuki.performSpecialRangedAttack(serverWorld, target, dist);
                        didAttack = true;
                    } else if (!aritsuki.rangedSequenceInProgress && aritsuki.rangedAttackCooldown <= 0) {
                        // Regular ranged attack when not in sequence and its own cooldown ready
                        aritsuki.performRangedAttack(serverWorld, target, dist);
                        didAttack = true;
                    }
                    if (!didAttack && aritsuki.rangedAttackCooldown > 0 && dist <= 15.0 && dist >= 6.0) {
                        // Debug why we skipped
                        LOGGER.debug("[RangedBehaviorGoal] Skipped regular shot: cooldown={} dist={}", aritsuki.rangedAttackCooldown, String.format("%.2f", dist));
                    }
                }
                this.attackTime = 0;
            }
        }
    }
    
    // Custom AI Goal for Summon behavior
    private static class AritsukiSummonBehaviorGoal extends Goal {
        private final AritsukiEntity aritsuki;
        private LivingEntity target;
        private int summonTime = -1;
        private boolean lastCanStart = false;
        
        public AritsukiSummonBehaviorGoal(AritsukiEntity aritsuki) {
            this.aritsuki = aritsuki;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            LivingEntity currentTarget = aritsuki.getTarget();
            boolean canStart = aritsuki.getCurrentMode() == CombatMode.SUMMON && 
                               currentTarget != null && currentTarget.isAlive();
            if (canStart != lastCanStart) {
                LOGGER.info("SummonBehaviorGoal canStart changed: {} (mode: {}, hasTarget: {})", 
                    canStart, aritsuki.getCurrentMode(), currentTarget != null);
                lastCanStart = canStart;
            }
            return canStart;
        }
        
        @Override
        public void start() {
            this.target = aritsuki.getTarget();
            this.summonTime = 0;
        }
        
        @Override
        public void stop() {
            this.target = null;
            this.summonTime = -1;
        }
        
        @Override
        public boolean shouldContinue() {
            return canStart();
        }
        
        @Override
        public void tick() {
            if (target == null) return;
            
            // Look at target but don't necessarily move
            aritsuki.getLookControl().lookAt(target, 30.0F, 30.0F);
            
            // Stop moving to focus on summoning
            aritsuki.getNavigation().stop();
            
            // Summon attack logic
            this.summonTime++;
            if (this.summonTime >= 100 && aritsuki.specialAttackCooldown <= 0) { // 5 second intervals
                if (!aritsuki.getWorld().isClient && aritsuki.getWorld() instanceof ServerWorld serverWorld) {
                    aritsuki.performSummonAttack(serverWorld, target);
                }
                this.summonTime = 0;
            }
        }
    }

    /* ===================== Attack Stub Methods ===================== */

    public void performDashAttack(ServerWorld world, LivingEntity target) {
    if (getCurrentMode() != CombatMode.MELEE) return; // only allow dash in melee mode
    if (dashInProgress || dashCooldownTicks > 0 || target == null || !target.isAlive()) return;
        dashInProgress = true;
        dashCooldownTicks = 60; // 3s @20tps between dashes
        this.getNavigation().stop();

        // Direction setup
        Vec3d rawForward = target.getPos().subtract(this.getPos());
        Vec3d forwardFlat = new Vec3d(rawForward.x, 0, rawForward.z);
        if (forwardFlat.lengthSquared() < 1.0E-4) {
            forwardFlat = this.getRotationVec(1.0f); // fallback
        }
        forwardFlat = forwardFlat.normalize();
        Vec3d right = new Vec3d(-forwardFlat.z, 0, forwardFlat.x); // perpendicular on horizontal plane

        final int entityId = this.getId(); // capture id to re-acquire safely in scheduled tasks

        // Ensure we are visually facing the target before starting movement impulses
        faceTargetInstant(target);

        // Trigger dash attack animation (client sync handled by GeckoLib)
        this.triggerAnim("dash", "dash_attack");
        LOGGER.debug("Dash animation trigger sent (controller=dash, key=dash_attack)");

        // Phase 0: initial forward + slight right burst
        this.addVelocity(forwardFlat.multiply(3).add(right.multiply(0.8)));
        spawnDashBurst(world, this.getPos(), 14, ParticleTypes.CLOUD);
        spawnDashBurst(world, this.getPos().add(0,0.1,0), 7, ParticleTypes.CRIT);

        // Phase 1 (5t): weave left across target line
        ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), 10, () -> {
            Entity self = world.getEntityById(entityId);
            if (!(self instanceof AritsukiEntity e) || !e.isAlive()) return;
            LivingEntity t = e.getTarget();
            if (t == null || !t.isAlive()) return;
            e.faceTargetInstant(t);
            Vec3d f = new Vec3d(t.getX() - e.getX(), 0, t.getZ() - e.getZ()).normalize();
            Vec3d r = new Vec3d(-f.z, 0, f.x);
            e.addVelocity(f.multiply(3).add(r.multiply(-1.6)));
            e.spawnDashTrail(world, e.getPos(), 10);
        });

        // Phase 2 (20t): vertical leap (anime hover) -- we want height without huge horizontal drift
        ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), 20, () -> {
            Entity self = world.getEntityById(entityId);
            if (!(self instanceof AritsukiEntity e) || !e.isAlive()) return;
            LivingEntity t = e.getTarget();
            if (t == null || !t.isAlive()) return;
            // Dampen existing horizontal velocity accumulated from earlier dash phases
            Vec3d current = e.getVelocity();
            Vec3d dampened = new Vec3d(current.x * 0.2, 0, current.z * 0.2);
            e.faceTargetInstant(t);
            // Approx initial vertical velocity to reach ~8 blocks: ~1.2-1.3 (player jump is 0.42 ~1.25 blocks)
            double vertical = 1.25; // tweak if needed
            // Optionally keep slight forward bias so it still feels directed
            Vec3d forwardBias = new Vec3d(t.getX() - e.getX(), 0, t.getZ() - e.getZ()).normalize().multiply(0.3);
            e.setVelocity(dampened.add(forwardBias.x, vertical, forwardBias.z));
            // Briefly negate gravity for a couple ticks to get a cleaner arc, then restore
            e.setNoGravity(true);
            ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), 3, () -> {
                Entity self2 = world.getEntityById(entityId);
                if (self2 instanceof AritsukiEntity e2) e2.setNoGravity(false);
            });
            e.spawnVerticalBurst(world, e.getPos().add(0,0.2,0));
        });

        // Phase 3 (15t): aerial re-align above target & slam
        ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), 30, () -> {
            Entity self = world.getEntityById(entityId);
            if (!(self instanceof AritsukiEntity e) || !e.isAlive()) return;
            LivingEntity t = e.getTarget();
            if (t == null || !t.isAlive()) { e.dashInProgress = false; return; }
            // Snap slightly above target then slam forward/down
            e.teleport(t.getX(), t.getY() + 1.2, t.getZ(), true);
            e.faceTargetInstant(t);
            Vec3d f = new Vec3d(t.getX() - e.getX(), 0, t.getZ() - e.getZ()).normalize();
            e.setVelocity(f.multiply(1.2).add(0, -1.2, 0));
            e.spawnChargeGather(world, e.getPos().add(0,0.15,0));
            // Attempt melee impact when landing shortly after (schedule minor damage window)
            ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), 1, () -> {
                Entity self2 = world.getEntityById(entityId);
                if (!(self2 instanceof AritsukiEntity e2) || !e2.isAlive()) return;
                LivingEntity t2 = e2.getTarget();
                if (t2 != null && t2.isAlive() && e2.squaredDistanceTo(t2) < 9.0) {
                    e2.faceTargetInstant(t2);
                    e2.tryAttack(world, t2);
                    e2.spawnImpactBurst(world, t2.getPos());
                }
            });
        });

        // Phase 4 (25t): cleanup end of dash
        ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), 35, () -> {
            Entity self = world.getEntityById(entityId);
            if (self instanceof AritsukiEntity e) {
                e.dashInProgress = false;
                e.setNoGravity(false);
            }
        });
        // Safety: ensure dashInProgress clears even if scheduled tasks missed (e.g., entity unloaded)
        ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), 80, () -> {
            Entity self = world.getEntityById(entityId);
            if (self instanceof AritsukiEntity e && e.dashInProgress) {
                e.dashInProgress = false;
                e.setNoGravity(false);
                LOGGER.warn("Dash safety reset executed (entityId={})", entityId);
            }
        });
        LOGGER.debug("DashAttack started (cooldown {} ticks)", dashCooldownTicks);
    }

    /** Instantly align yaw/head/body toward the given living target (server authoritative). */
    private void faceTargetInstant(LivingEntity target) {
        if (target == null) return;
    double dx = target.getX() - this.getX();
    double dz = target.getZ() - this.getZ();
    // Standard Minecraft yaw: atan2(dz, dx) * 180/PI - 90
    float desiredYaw = (float)(MathHelper.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
    // Normalize to (-180,180]
    while (desiredYaw > 180F) desiredYaw -= 360F;
    while (desiredYaw <= -180F) desiredYaw += 360F;
    // Apply directly (instant). Could smooth by lerp if needed.
    this.setYaw(desiredYaw);
    this.setHeadYaw(desiredYaw);
    this.setBodyYaw(desiredYaw);
    }

    // ===================== Particle Helpers =====================
    private void spawnDashBurst(ServerWorld world, Vec3d center, int count, ParticleEffect type) {
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * Math.PI * 2;
            double radius = 0.35 + (i % 3) * 0.05;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            world.spawnParticles(type, center.x + dx, center.y + 0.05, center.z + dz, 1, 0, 0, 0, 0.0);
        }
    }

    private void spawnDashTrail(ServerWorld world, Vec3d pos, int count) {
        for (int i = 0; i < count; i++) {
            double ox = (this.getRandom().nextDouble() - 0.5) * 0.6;
            double oy = this.getRandom().nextDouble() * 0.25;
            double oz = (this.getRandom().nextDouble() - 0.5) * 0.6;
            world.spawnParticles(ParticleTypes.CRIT, pos.x + ox, pos.y + 0.1 + oy, pos.z + oz, 1, 0, 0, 0, 0.0);
        }
    }

    private void spawnVerticalBurst(ServerWorld world, Vec3d pos) {
        for (int i = 0; i < 12; i++) {
            double vx = (this.getRandom().nextDouble() - 0.5) * 0.12;
            double vz = (this.getRandom().nextDouble() - 0.5) * 0.12;
            double vy = 0.25 + this.getRandom().nextDouble() * 0.35;
            world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 1, vx, vy, vz, 0.0);
        }

        ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), 3, () -> {
            for (int i = 0; i < 24; i++) {
                double vx = (this.getRandom().nextDouble() - 0.5) * 1;
                double vz = (this.getRandom().nextDouble() - 0.5) * 1;
                double vy = 0.25 + this.getRandom().nextDouble() * 0.70;
                world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 1, vx, vy, vz, 0.0);
            }
        });
    }

    private void spawnChargeGather(ServerWorld world, Vec3d pos) {
        for (int i = 0; i < 18; i++) {
            double angle = (i / 18.0) * Math.PI * 2;
            double radius = 0.65;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.ENCHANT, pos.x + dx, pos.y + 0.2, pos.z + dz, 1, 0, 0, 0, 0.0);
        }
    }

    private void spawnImpactBurst(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 1.0, pos.z, 6, 0.2, 0.1, 0.2, 0.0);
        for (int i = 0; i < 14; i++) {
            double vx = (this.getRandom().nextDouble() - 0.5) * 0.7;
            double vy = this.getRandom().nextDouble() * 0.5;
            double vz = (this.getRandom().nextDouble() - 0.5) * 0.7;
            world.spawnParticles(ParticleTypes.CRIT, pos.x, pos.y + 1.0, pos.z, 1, vx, vy, vz, 0.0);
        }
    }

    public void performSpecialMeleeAttack(ServerWorld world, LivingEntity target) {
        performWindRingAttack(world, 45); // Example default effect; remove if undesired
    }

    public void performRangedAttack(ServerWorld world, LivingEntity target, double distanceToTarget) {
        // Basic short-range arrow shot (no fancy ring). Only when actually in ranged mode.
        if (this.getCurrentMode() != CombatMode.RANGED) return;
        if (target == null || !target.isAlive()) return;
        // Distance gate (allow mid-range; skip if too close or too far for this basic shot)
        if (distanceToTarget < 4.0 || distanceToTarget > 15.0) {
            return;
        }
        if (this.rangedAttackCooldown > 0) {
            LOGGER.trace("[RangedAttack-basic] Cooldown active {} ticks remaining", rangedAttackCooldown);
            return; // still cooling down
        }

        // Set a modest cooldown so goal logic doesn't spam; tune as desired.
        this.rangedAttackCooldown = 40; // 2 seconds @20tps
        LOGGER.debug("[RangedAttack-basic] Firing basic arrow at dist={} cooldown set=40", String.format("%.2f", distanceToTarget));

        // Particle telegraph on target
        world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY() - 0.2, target.getZ(), 4, 0.05, 0.05, 0.05, 0.0);

        // Compute direction from shooter eye to target eye
        double sx = this.getX();
        double sy = this.getEyeY() - 0.1; // slight offset
        double sz = this.getZ();
        double tx = target.getX();
        double ty = target.getEyeY();
        double tz = target.getZ();
        double vx = tx - sx;
        double vy = ty - sy;
        double vz = tz - sz;

        // Use helper to spawn and launch arrow
        ArrowEntity spawned = spawnWithVelocity(
            (srvWorld, stack, shooter) -> {
                ArrowEntity a = EntityType.ARROW.create(srvWorld, SpawnReason.TRIGGERED);
                if (a != null) {
                    a.setCritical(true);
                }
                return a;
            },
            world,
            Items.ARROW.getDefaultStack(),
            this,
            vx, vy, vz,
            1.6f, // power (initial speed multiplier)
            0.15f // divergence (small inaccuracy)
        );

        if (spawned != null) {
            world.playSound(null, spawned.getX(), spawned.getY(), spawned.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1.0f, 0.9f + this.getRandom().nextFloat() * 0.2f);
        }
    }

    /**
     * Generic helper to spawn a projectile using a creator and apply an initial velocity with power & divergence.
     * Divergence adds Gaussian spread similar to vanilla bow mechanics. Velocity components (vx,vy,vz) provide the intended shot direction.
     */
    private static <T extends ProjectileEntity> T spawnWithVelocity(ProjectileEntity.ProjectileCreator<T> creator,
                                                                    ServerWorld world,
                                                                    ItemStack projectileStack,
                                                                    LivingEntity shooter,
                                                                    double vx, double vy, double vz,
                                                                    float power,
                                                                    float divergence) {
        if (creator == null || world == null || shooter == null) return null;
    // Fabric/MC ProjectileCreator signature: create(ServerWorld world, LivingEntity shooter, ItemStack stack)
    T projectile = creator.create(world, shooter, projectileStack);
        if (projectile == null) return null;
        // Position at shooter eye
        projectile.refreshPositionAndAngles(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ(), shooter.getYaw(), shooter.getPitch());
        projectile.setOwner(shooter);

        double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (len < 1.0E-7) {
            // fallback to look vector
            Vec3d look = shooter.getRotationVec(1.0f);
            vx = look.x;
            vy = look.y;
            vz = look.z;
            len = Math.sqrt(vx * vx + vy * vy + vz * vz);
        }
        vx /= len; vy /= len; vz /= len;

        // Apply divergence Gaussian noise (same constant 0.0075 used internally by vanilla)
    java.util.Random jRandom = new java.util.Random();
    double spreadScale = 0.0075 * divergence;
    vx += jRandom.nextGaussian() * spreadScale;
    vy += jRandom.nextGaussian() * spreadScale;
    vz += jRandom.nextGaussian() * spreadScale;

        // Re-normalize after divergence
        double len2 = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (len2 > 1.0E-7) {
            vx /= len2; vy /= len2; vz /= len2;
        }
        projectile.setVelocity(vx * power, vy * power, vz * power);
        world.spawnEntity(projectile);
        return projectile;
    }

    /**
     * Called by the ranged behavior goal when it's time to perform a ranged attack.
     * Implement your projectile spawning, particle effects, sounds, etc. here.
     * Manage and set {@code specialRangedAttackCooldown} inside this method as needed.
     *
     * @param world   Server world
     * @param target  Current attack target (guaranteed non-null & alive when called)
     * @param distanceToTarget Current straight-line distance (not squared)
     */
    public void performSpecialRangedAttack(ServerWorld world, LivingEntity target, double distanceToTarget) {
        LOGGER.debug("[RangedAttack] Entry: target={} dist={} cooldown={} mode={}",
            target.getName().getString(), String.format("%.2f", distanceToTarget), this.specialRangedAttackCooldown, this.getCurrentMode());
        if (rangedSequenceInProgress) {
            LOGGER.debug("[RangedAttack] Aborted: sequence already in progress");
            return;
        }
        if (this.getCurrentMode() != CombatMode.RANGED) {
            LOGGER.debug("[RangedAttack] Aborted early: mode now {}", this.getCurrentMode());
            return;
        }

        // Special Ranged attack behavior:
        // 1. Lift slightly & hover (disable gravity temporarily)
        // 2. After warm-up, spawn a vertical "standing" ring (wheel) of arrows whose plane faces the target.
        // 3. After a delay, fire arrows sequentially toward the target eyes every 2 ticks.
        // 4. Restore gravity & end hover after sequence or if mode/target invalidated.

        // Immediate cooldown lock to avoid overlapping scheduling from rapid goal ticks
        this.specialRangedAttackCooldown = 80; // ~4s; tune as desired
        this.rangedSequenceInProgress = true;

        // Stop moving & begin hover
        this.getNavigation().stop();
        this.setNoGravity(true);
        // Gentle lift toward ~2 blocks above ground over warm-up period
        this.setVelocity(0, 0.15, 0);

        final int selfId = this.getId();
        final int targetId = target.getId();
        final int arrowCount = 8;
        final double ringRadius = 0.9; // visual size
        final int ringSpawnDelay = 20;  // ticks until ring appears
        final int fireStartDelay = ringSpawnDelay + 20; // 1s after ring spawn
        final int fireSpacing = 4; // ticks between each arrow firing
        final int gravityRestoreDelay = fireStartDelay + fireSpacing * (arrowCount - 1) + 10;
        final int arrowLifetimeTicks = 120; // failsafe lifetime for each spawned arrow (~6s)

        List<Integer> arrowIds = new ArrayList<>(arrowCount);

        // Helper lambda to reacquire entity safely
        java.util.function.Supplier<AritsukiEntity> selfSupplier = () -> {
            Entity e = world.getEntityById(selfId);
            return e instanceof AritsukiEntity ae && ae.isAlive() ? ae : null;
        };

        // Spawn the ring (wheel) of arrows in a plane facing the target
        ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), ringSpawnDelay, () -> {
            AritsukiEntity self = selfSupplier.get();
            if (self == null) return;
            self.setVelocity(0, 0, 0);
            Entity tgtEntity = world.getEntityById(targetId);
            if (!(tgtEntity instanceof LivingEntity liveTgt) || !liveTgt.isAlive()) {
                LOGGER.debug("[RangedAttack] Ring spawn aborted: target invalid (id={})", targetId);
                self.setNoGravity(false);
                self.rangedSequenceInProgress = false;
                // Allow a faster retry since the attempt never launched
                if (self.specialRangedAttackCooldown > 20) self.specialRangedAttackCooldown = 20;
                return;
            }
            if (self.getCurrentMode() != CombatMode.RANGED) {
                LOGGER.debug("[RangedAttack] Ring spawn aborted: mode changed to {} before spawn", self.getCurrentMode());
                self.setNoGravity(false);
                self.rangedSequenceInProgress = false;
                return; // cancelled due to mode change
            }
            LOGGER.debug("[RangedAttack] Spawning arrow ring: center=({}, {}, {}), targetDist={}",
                String.format("%.2f", self.getX()), String.format("%.2f", self.getY()+1.8), String.format("%.2f", self.getZ()), String.format("%.2f", distanceToTarget));

            Vec3d forward = liveTgt.getPos().subtract(self.getPos()).normalize();
            // Build orthonormal basis for ring plane (forward is normal). Use world up for stability.
            Vec3d up = new Vec3d(0, 1, 0);
            Vec3d right = forward.crossProduct(up);
            if (right.lengthSquared() < 1.0E-4) {
                // Edge case: forward parallel with up; pick arbitrary right
                right = new Vec3d(1, 0, 0);
            }
            right = right.normalize();
            Vec3d ringUp = right.crossProduct(forward).normalize(); // ensures orthogonality

            // Lower the ring center a bit so arrows don't have to shoot steeply downward
            double centerYOffset = 1.2; // was 1.8
            double targetEyeY = liveTgt.getEyeY();
            // Keep ring roughly near or slightly above target eyes (avoid being far above causing downward shots)
            double desiredCenterY = Math.min(self.getY() + centerYOffset, targetEyeY + 1.5);
            Vec3d center = new Vec3d(self.getX() + forward.x * 0.2, desiredCenterY, self.getZ() + forward.z * 0.2);

            for (int i = 0; i < arrowCount; i++) {
                double theta = (i / (double) arrowCount) * Math.PI * 2;
                Vec3d offset = right.multiply(Math.cos(theta) * ringRadius).add(ringUp.multiply(Math.sin(theta) * ringRadius));
                Vec3d arrowPos = center.add(offset);
                ArrowEntity arrow = EntityType.ARROW.create(world, SpawnReason.TRIGGERED);
                if (arrow != null) {
                    arrow.refreshPositionAndAngles(arrowPos.x, arrowPos.y, arrowPos.z, 0.0F, 0.0F);
                    arrow.setOwner(self);
                    arrow.setNoGravity(true);
                    arrow.setCritical(true);
                    world.spawnEntity(arrow);
                    arrowIds.add(arrow.getId());
                    final int spawnedArrowId = arrow.getId();
                    // Schedule lifetime despawn (removal) as a safety net
                    ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), arrowLifetimeTicks, () -> {
                        Entity possible = world.getEntityById(spawnedArrowId);
                        if (possible instanceof ArrowEntity a && a.isAlive()) {
                            a.discard();
                            LOGGER.debug("[RangedAttack] Arrow lifetime expired -> despawn id={}", spawnedArrowId);
                        }
                    });
                    if (i == 0 || i == arrowCount-1) {
                        LOGGER.debug("[RangedAttack] Arrow spawned idx={} pos=({}, {}, {}) id={}", i,
                            String.format("%.2f", arrowPos.x), String.format("%.2f", arrowPos.y), String.format("%.2f", arrowPos.z), arrow.getId());
                    }
                } else {
                    LOGGER.warn("[RangedAttack] ArrowEntity.create returned null (idx={})", i);
                }
            }
            LOGGER.debug("[RangedAttack] Ring spawn complete. Spawned {} arrows.", arrowIds.size());
            world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 1.0f, 0.8f + self.getRandom().nextFloat() * 0.4f);
        });

        // Sequential firing
        for (int i = 0; i < arrowCount; i++) {
            final int idx = i;
            int fireTick = fireStartDelay + fireSpacing * idx;
            ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), fireTick, () -> {
                AritsukiEntity self = selfSupplier.get();
                if (self == null) return;
                // Restore gravity early if mode changed mid-sequence
                if (self.getCurrentMode() != CombatMode.RANGED) return;
                Entity tgtEntity = world.getEntityById(targetId);
                if (!(tgtEntity instanceof LivingEntity liveTgt) || !liveTgt.isAlive()) return;
                if (idx >= arrowIds.size()) return;
                Entity arrowEnt = world.getEntityById(arrowIds.get(idx));
                if (!(arrowEnt instanceof ArrowEntity arrow) || !arrow.isAlive()) return;

                // Aim toward target eye with slight upward compensation if we're above the target
                double targetEyeY = liveTgt.getEyeY();
                Vec3d arrowPos = arrow.getPos();
                double dy = targetEyeY - arrowPos.y;
                // If arrow starts noticeably above eyes, reduce downward component (add upward bias)
                double upwardBias = 0.0;
                if (dy < -0.4) { // arrow above target eyes
                    upwardBias = Math.min(0.4, (-dy) * 0.35); // partial compensation
                }
                Vec3d aimPoint = new Vec3d(liveTgt.getX(), targetEyeY + upwardBias, liveTgt.getZ());
                Vec3d dir = aimPoint.subtract(arrowPos);
                // Add very small lead based on target motion (if any)
                Vec3d tgtVel = liveTgt.getVelocity();
                dir = dir.add(tgtVel.x * 0.5, tgtVel.y * 0.2, tgtVel.z * 0.5);
                dir = dir.normalize();
                double speed = 2.4; // faster to flatten trajectory
                arrow.setVelocity(dir.x * speed, dir.y * speed, dir.z * speed);
                // Keep noGravity for a few ticks so arrow travels straight, then enable gravity
                arrow.setNoGravity(true);
                final int aId = arrow.getId();
                ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), 8, () -> {
                    Entity e2 = world.getEntityById(aId);
                    if (e2 instanceof ArrowEntity a2 && a2.isAlive()) {
                        a2.setNoGravity(false);
                    }
                });
                world.playSound(null, arrow.getX(), arrow.getY(), arrow.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1.0f, 0.9f + self.getRandom().nextFloat() * 0.2f);
                // Minor particle flair each launch
                world.spawnParticles(ParticleTypes.CRIT, arrow.getX(), arrow.getY(), arrow.getZ(), 3, 0.05, 0.05, 0.05, 0.0);
                if (idx == 0 || idx == arrowCount-1) {
                    LOGGER.debug("[RangedAttack] Fired arrow idx={} id={} vel=({}, {}, {})", idx, arrow.getId(),
                        String.format("%.2f", arrow.getVelocity().x), String.format("%.2f", arrow.getVelocity().y), String.format("%.2f", arrow.getVelocity().z));
                }
            });
        }

        // Gravity restore & cleanup safeguard
        ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), gravityRestoreDelay, () -> {
            AritsukiEntity self = selfSupplier.get();
            if (self != null) {
                self.setNoGravity(false);
                self.rangedSequenceInProgress = false;
                LOGGER.debug("[RangedAttack] Sequence cleanup complete (gravity restored)");
            }
        });

        // Fail-safe: if for any reason cleanup didn't run (entity unloaded mid sequence, etc.)
        // ensure gravity & flag are restored after an upper bound (~10s).
        ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), gravityRestoreDelay + 140, () -> {
            AritsukiEntity self = selfSupplier.get();
            if (self != null && self.rangedSequenceInProgress) {
                self.setNoGravity(false);
                self.rangedSequenceInProgress = false;
                LOGGER.warn("[RangedAttack] Fail-safe cleanup executed (sequence flag reset)");
            }
        });

        LOGGER.info("Ranged attack scheduled (dist={} arrows={} warmup={} fireStart={} spacing={} restore={})", String.format("%.2f", distanceToTarget), arrowCount, ringSpawnDelay, fireStartDelay, fireSpacing, gravityRestoreDelay);
    }

    /**
     * Called by the summon behavior goal at its interval. Implement minion spawning,
     * area attacks, or special effects. Manage and set {@code specialAttackCooldown}
     * inside this method as needed.
     *
     * @param world  Server world
     * @param target Current target
     */
    public void performSummonAttack(ServerWorld world, LivingEntity target) {
        // Null-safe target usage (can be null if no target set)
        int summonDistance = 5;
        int toSpawn = Math.max(1, summonCount);
        for (int i = 0; i < toSpawn; i++) {
            double angle = (i / (double) toSpawn) * 2 * Math.PI;
            double x = this.getX() + Math.cos(angle) * summonDistance;
            double z = this.getZ() + Math.sin(angle) * summonDistance;
            spawnMinion(world, x, this.getY(), z);
        }
        summonCount++;
        this.specialAttackCooldown = Math.max(this.specialAttackCooldown, 200); // ensure cooldown set
        LOGGER.info("performSummonAttack invoked: spawned {} minions (count now {}), next threshold {} ticks.", toSpawn, summonCount, getNextSummonThresholdTicks());
    }

    private void spawnMinion(ServerWorld world, double x, double y, double z) {
        LivingEntity minion;
        Random random = new Random();
        random.nextInt(6);

        switch (random.nextInt(6)) {
            case 0:
                minion = EntityType.DROWNED.create(world, SpawnReason.TRIGGERED);
                break;
            case 1:
                minion = EntityType.ZOMBIE.create(world, SpawnReason.TRIGGERED);
                break;
            case 2:
                minion = EntityType.SKELETON.create(world, SpawnReason.TRIGGERED);
                break;
            case 3:
                minion = EntityType.HUSK.create(world, SpawnReason.TRIGGERED);
                break;
            case 4:
                minion = EntityType.BOGGED.create(world, SpawnReason.TRIGGERED);
                break;
            case 5:
                minion = EntityType.STRAY.create(world, SpawnReason.TRIGGERED);
                break;
            default:
                minion = EntityType.ZOMBIE.create(world, SpawnReason.TRIGGERED);
                break;
        }

        minion.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
        world.spawnEntity(minion);
    }

    private void performWindRingAttack(ServerWorld world, int stepDegrees) {
        final double y = this.getY() + 1.0;
        final double startRadius = 3.0;
        final double endRadius = 8.0;
        final int totalRotations = 2;
        final int totalDegrees = 360 * totalRotations;
        final int DELAY_BETWEEN_CHARGES = 2;
        
        int chargeIndex = 0;
        for (int deg = 0; deg < totalDegrees; deg += stepDegrees) {
            double progress = (double) deg / totalDegrees;
            double radius = startRadius + (endRadius - startRadius) * progress;
            
            double rad = Math.toRadians(deg);
            double x = this.getX() + Math.cos(rad) * radius;
            double z = this.getZ() + Math.sin(rad) * radius;

            ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), chargeIndex * DELAY_BETWEEN_CHARGES, () -> {
                spawnSingleWindCharge(world, this, x, y, z);
            });            
            chargeIndex++;
        }
    }

    /** Compute ticks required until the next summon wave. */
    private int getNextSummonThresholdTicks() {
        // Base interval 100 ticks (5s) scaled by multiplier and current summonCount
        return (int) (200 * (summonTimeMultiplier * summonCount));
    }
	
    private void spawnSingleWindCharge(ServerWorld world, LivingEntity user, double x, double y, double z) {
        WindChargeEntity charge = EntityType.WIND_CHARGE.create(world, SpawnReason.TRIGGERED);
        if (charge == null) return;

        charge.setOwner(user);
        charge.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
        charge.setVelocity(0.0, -1.0, 0.0);
        world.spawnEntity(charge);
    }
}