package com.voltaicgrid.chatplaysmc.mixin;

import com.voltaicgrid.chatplaysmc.client.LockOnManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityLockOnMixin {
   @Unique
   private int chat_plays_mc$lockTargetId = -1;
   @Unique
   private static final double chat_plays_mc$RANGE = 64.0D;
   @Unique
   private List<Entity> chat_plays_mc$cachedTargets = new ArrayList();
   @Unique
   private int chat_plays_mc$targetIndex = -1;
   @Unique
   private int chat_plays_mc$cacheUpdateTicks = 0;
   @Unique
   private static final int chat_plays_mc$CACHE_UPDATE_INTERVAL = 20;

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void chat_plays_mc$lockOnTick(CallbackInfo ci) {
	  ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
      ClientWorld world = (ClientWorld)player.getWorld();
      if (!LockOnManager.isEnabled()) {
         this.chat_plays_mc$lockTargetId = -1;
         this.chat_plays_mc$targetIndex = -1;
         this.chat_plays_mc$cachedTargets.clear();
      } else {
         String typeKey = LockOnManager.getEntityTypeKey();
         if (typeKey != null && !typeKey.isEmpty()) {
            ++this.chat_plays_mc$cacheUpdateTicks;
            if (this.chat_plays_mc$cacheUpdateTicks >= 20) {
               this.chat_plays_mc$updateTargetCache(world, player, typeKey);
               this.chat_plays_mc$cacheUpdateTicks = 0;
            }

            String command = LockOnManager.getTargetingCommand();
            if (command != null) {
               this.chat_plays_mc$handleTargetingCommand(command, player, world, typeKey);
               LockOnManager.clearTargetingCommand();
            }

            Entity target = world.getEntityById(this.chat_plays_mc$lockTargetId);
            if (!this.chat_plays_mc$isValidTarget(player, target, typeKey)) {
               if (this.chat_plays_mc$cachedTargets.isEmpty()) {
                  this.chat_plays_mc$updateTargetCache(world, player, typeKey);
               }

               if (!this.chat_plays_mc$cachedTargets.isEmpty()) {
                  target = (Entity)this.chat_plays_mc$cachedTargets.get(0);
                  this.chat_plays_mc$lockTargetId = target.getId();
                  this.chat_plays_mc$targetIndex = 0;
               } else {
                  this.chat_plays_mc$lockTargetId = -1;
                  this.chat_plays_mc$targetIndex = -1;
               }
            }

            if (target != null) {
               this.chat_plays_mc$face(player, (LivingEntity)target);
            }

         }
      }
   }

   @Unique
   private void chat_plays_mc$handleTargetingCommand(String command, ClientPlayerEntity player, ClientWorld world, String typeKey) {
      String var5 = command.toLowerCase();
      byte var6 = -1;
      switch(var5.hashCode()) {
      case -1273775369:
         if (var5.equals("previous")) {
            var6 = 1;
         }
         break;
      case 3377907:
         if (var5.equals("next")) {
            var6 = 0;
         }
         break;
      case 3449395:
         if (var5.equals("prev")) {
            var6 = 2;
         }
         break;
      case 1825779806:
         if (var5.equals("nearest")) {
            var6 = 3;
         }
      }

      switch(var6) {
      case 0:
         this.chat_plays_mc$targetNext(player, world, typeKey);
         break;
      case 1:
      case 2:
         this.chat_plays_mc$targetPrevious(player, world, typeKey);
         break;
      case 3:
         this.chat_plays_mc$targetNearest(player, world, typeKey);
      }

   }

   @Unique
   private void chat_plays_mc$targetNext(ClientPlayerEntity player, ClientWorld world, String typeKey) {
      if (this.chat_plays_mc$cachedTargets.isEmpty()) {
         this.chat_plays_mc$updateTargetCache(world, player, typeKey);
      }

      if (!this.chat_plays_mc$cachedTargets.isEmpty()) {
         this.chat_plays_mc$targetIndex = (this.chat_plays_mc$targetIndex + 1) % this.chat_plays_mc$cachedTargets.size();
         Entity target = (Entity)this.chat_plays_mc$cachedTargets.get(this.chat_plays_mc$targetIndex);
         this.chat_plays_mc$lockTargetId = target.getId();
         this.chat_plays_mc$sendTargetMessage(player, target, "Next");
      }

   }

   @Unique
   private void chat_plays_mc$targetPrevious(ClientPlayerEntity player, ClientWorld world, String typeKey) {
      if (this.chat_plays_mc$cachedTargets.isEmpty()) {
         this.chat_plays_mc$updateTargetCache(world, player, typeKey);
      }

      if (!this.chat_plays_mc$cachedTargets.isEmpty()) {
         --this.chat_plays_mc$targetIndex;
         if (this.chat_plays_mc$targetIndex < 0) {
            this.chat_plays_mc$targetIndex = this.chat_plays_mc$cachedTargets.size() - 1;
         }

         Entity target = (Entity)this.chat_plays_mc$cachedTargets.get(this.chat_plays_mc$targetIndex);
         this.chat_plays_mc$lockTargetId = target.getId();
         this.chat_plays_mc$sendTargetMessage(player, target, "Previous");
      }

   }

   @Unique
   private void chat_plays_mc$targetNearest(ClientPlayerEntity player, ClientWorld world, String typeKey) {
      Entity nearest = this.chat_plays_mc$findNearest(world, player, typeKey, 64.0D);
      if (nearest != null) {
         this.chat_plays_mc$lockTargetId = nearest.getId();
         this.chat_plays_mc$updateTargetCache(world, player, typeKey);

         for(int i = 0; i < this.chat_plays_mc$cachedTargets.size(); ++i) {
            if (((Entity)this.chat_plays_mc$cachedTargets.get(i)).getId() == nearest.getId()) {
               this.chat_plays_mc$targetIndex = i;
               break;
            }
         }

         this.chat_plays_mc$sendTargetMessage(player, nearest, "Nearest");
      }

   }

   @Unique
   private void chat_plays_mc$updateTargetCache(ClientWorld world, ClientPlayerEntity player, String typeKey) {
      EntityType<?> want = this.chat_plays_mc$resolveType(typeKey);
      if (want == null) {
         // If the requested type doesn't resolve, clear our cache and bail out.
         this.chat_plays_mc$cachedTargets.clear();
         return;
      }

      // Define a bounding box around the player to search for entities in.
      Box box = new Box(
         player.getX() - 64.0D, player.getY() - 64.0D, player.getZ() - 64.0D,
         player.getX() + 64.0D, player.getY() + 64.0D, player.getZ() + 64.0D
      );

      // Get all living entities of the desired type that are alive within the box.
      List<LivingEntity> candidates = world.getEntitiesByClass(
         LivingEntity.class,
         box,
         e -> e.getType() == want && e.isAlive()
      );

      // Sort the candidates by distance to the player and convert to a list of Entity.
      List<Entity> sorted = candidates.stream()
         .sorted(Comparator.comparingDouble(e -> player.squaredDistanceTo(e)))
         .map(e -> (Entity) e)
         .collect(Collectors.toList());

      this.chat_plays_mc$cachedTargets = sorted;

      // If we already had a locked target, update the target index in the new list.
      if (this.chat_plays_mc$lockTargetId != -1) {
         boolean found = false;
         for (int i = 0; i < this.chat_plays_mc$cachedTargets.size(); ++i) {
            if (this.chat_plays_mc$cachedTargets.get(i).getId() == this.chat_plays_mc$lockTargetId) {
               this.chat_plays_mc$targetIndex = i;
               found = true;
               break;
            }
         }
         if (!found) {
            this.chat_plays_mc$targetIndex = -1;
         }
      }
   }

   @Unique
   private void chat_plays_mc$sendTargetMessage(ClientPlayerEntity player, Entity target, String action) {
      String targetName = target.getName().getString();
      double distance = Math.sqrt(target.squaredDistanceTo(player));
      int targetNumber = this.chat_plays_mc$targetIndex + 1;
      int totalTargets = this.chat_plays_mc$cachedTargets.size();
      Text message = Text.literal(String.format("§6%s target: §f%s §7(%.1fm) §8[%d/%d]", action, targetName, distance, targetNumber, totalTargets));
      player.sendMessage(message, true);
   }

   @Unique
   private boolean chat_plays_mc$isValidTarget(ClientPlayerEntity player, Entity e, String typeKey) {
      if (e instanceof LivingEntity) {
         LivingEntity le = (LivingEntity)e;
         if (!e.isRemoved() && e.isAlive()) {
            if (!this.chat_plays_mc$typeMatches(e.getType(), typeKey)) {
               return false;
            }

            return e.squaredDistanceTo(player) <= 4096.0D;
         }
      }

      return false;
   }

   @Unique
   private Entity chat_plays_mc$findNearest(ClientWorld world, ClientPlayerEntity player, String typeKey, double range) {
      EntityType<?> want = this.chat_plays_mc$resolveType(typeKey);
      if (want == null) {
         return null;
      } else {
         Box box = new Box(player.getX() - range, player.getY() - range, player.getZ() - range, player.getX() + range, player.getY() + range, player.getZ() + range);
         List<LivingEntity> candidates = world.getEntitiesByClass(
		    LivingEntity.class, box,
		    e -> e.getType() == want && e.isAlive()
		);
         Objects.requireNonNull(player);
         return candidates.stream()
        		    .min(Comparator.comparingDouble(e -> player.squaredDistanceTo(e)))
        		    .orElse(null);
         }
   }

   @Unique
   private boolean chat_plays_mc$typeMatches(EntityType<?> type, String typeKey) {
      EntityType<?> want = this.chat_plays_mc$resolveType(typeKey);
      return want != null && type == want;
   }

   @Unique
   private EntityType<?> chat_plays_mc$resolveType(String typeKey) {
       if (typeKey == null || typeKey.isEmpty()) {
           return null;
       }

       Identifier id = Identifier.tryParse(typeKey.toLowerCase(Locale.ROOT));
       if (id == null) {
           return null; // not a valid identifier
       }

       return Registries.ENTITY_TYPE.get(id);
   }

   @Unique
   private void chat_plays_mc$face(ClientPlayerEntity player, LivingEntity target) {
      Vec3d playerEye = player.getEyePos();
      Vec3d targetEye = target.getEyePos();
      Vec3d diff = targetEye.subtract(playerEye);
      double dx = diff.x;
      double dy = diff.y;
      double dz = diff.z;
      double distXZ = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
      float pitch = (float)Math.toDegrees(-Math.atan2(dy, distXZ));
      pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
      player.setYaw(yaw);
      player.setPitch(pitch);
      player.bodyYaw = yaw;
   }
}
