package com.voltaicgrid.chatplaysmc.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;

@Environment(EnvType.CLIENT)
public final class LockOnManager {
   private static volatile boolean enabled = false;
   private static volatile String entityTypeKey = "";
   private static String targetingCommand = null;
   private static boolean movingToTarget = false;
   private static double targetMoveDistance = 3.0D;
   // Shared current target (entity id) so camera and mixins can coordinate
   private static volatile int currentTargetId = -1;

   private LockOnManager() {}

   public static boolean isEnabled() { return enabled; }

   public static void setEnabled(boolean value) {
      enabled = value;
      if (!enabled) {
         movingToTarget = false;
         clearCurrentTarget();
      }
   }

   public static String getEntityTypeKey() { return entityTypeKey; }

   public static void setEntityTypeKey(String key) { if (key != null) entityTypeKey = key; }

   // Return the currently selected target in this world, or null
   public static Entity getCurrentTarget(ClientWorld world) {
      if (world == null || currentTargetId == -1) return null;
      return world.getEntityById(currentTargetId);
   }

   // Set/clear current target
   public static void setCurrentTarget(Entity target) { currentTargetId = target != null ? target.getId() : -1; }
   public static void setCurrentTargetId(int id) { currentTargetId = id; }
   public static void clearCurrentTarget() { currentTargetId = -1; }

   public static void setTargetingCommand(String command) { targetingCommand = command; }
   public static String getTargetingCommand() { return targetingCommand; }
   public static void clearTargetingCommand() { targetingCommand = null; }

   public static boolean isMovingToTarget() { return movingToTarget; }
   public static void setMovingToTarget(boolean moving) { movingToTarget = moving; }

   public static double getTargetMoveDistance() { return targetMoveDistance; }
   public static void setTargetMoveDistance(double distance) { targetMoveDistance = Math.max(1.0D, distance); }
}