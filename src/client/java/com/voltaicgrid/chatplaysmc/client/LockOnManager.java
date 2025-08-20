package com.voltaicgrid.chatplaysmc.client;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class LockOnManager {
    private static boolean enabled = false;
    private static String entityTypeKey = "";
    private static int currentTargetId = -1;
    private static String targetingCommand = "";
    private static float distanceToTarget = 0;
    
    public static void setEnabled(boolean enabled) {
        LockOnManager.enabled = enabled;
        if (!enabled) {
            currentTargetId = -1;
        }
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setEntityTypeKey(String entityType) {
        LockOnManager.entityTypeKey = entityType.toLowerCase();
        // Clear current target when changing entity type
        currentTargetId = -1;
    }
    
    public static void setDistanceToTarget(float distance) {
		LockOnManager.distanceToTarget = distance;
	}
    
    public static float getDistanceToTarget() {
		return distanceToTarget;
	}
    
    public static void getEntityTypeName() {
    	if (entityTypeKey.isEmpty()) {
			System.out.println("No specific entity type set for targeting.");
		} else {
			System.out.println("Current entity type key: " + entityTypeKey);
		}
    }
    
    public static String getEntityTypeKey() {
        return entityTypeKey;
    }
    
    public static void setTargetingCommand(String command) {
        LockOnManager.targetingCommand = command.toLowerCase();
    }
    
    public static String getTargetingCommand() {
        return targetingCommand;
    }
    
    public static void clearTargetingCommand() {
        targetingCommand = "";
    }
    
    public static Entity getCurrentTarget(ClientWorld world) {
        if (!enabled || currentTargetId == -1 || world == null) {
            return null;
        }
        
        // Find entity by ID
        return world.getEntityById(currentTargetId);
    }
    
    public static void updateTargeting(ClientWorld world, PlayerEntity player) {
        if (!enabled || world == null || player == null || targetingCommand.isEmpty()) {
            return;
        }
        
        switch (targetingCommand) {
            case "nearest" -> targetNearest(world, player);
            case "next" -> targetNext(world, player);
            case "previous", "prev" -> targetPrevious(world, player);
        }
        
        // Clear the command after executing
        targetingCommand = "";
    }
    
    private static void targetNearest(ClientWorld world, PlayerEntity player) {
        List<Entity> candidates = getTargetableCandidates(world, player);
        if (candidates.isEmpty()) return;
        
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Vec3d playerPos = player.getPos();
        
        for (Entity entity : candidates) {
            double distance = entity.getPos().distanceTo(playerPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = entity;
            }
        }
        
        if (nearest != null) {
            currentTargetId = nearest.getId();
            System.out.println("Locked onto nearest: " + nearest.getName().getString() + " (ID: " + nearest.getId() + ")");
        }
    }
    
    private static void targetNext(ClientWorld world, PlayerEntity player) {
        List<Entity> candidates = getTargetableCandidates(world, player);
        if (candidates.isEmpty()) return;
        
        // Sort by distance for consistent ordering
        candidates.sort((a, b) -> Double.compare(
            a.getPos().distanceTo(player.getPos()),
            b.getPos().distanceTo(player.getPos())
        ));
        
        if (currentTargetId == -1) {
            // No current target, select first
            currentTargetId = candidates.get(0).getId();
            System.out.println("Locked onto first: " + candidates.get(0).getName().getString());
            return;
        }
        
        // Find current target index and select next
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).getId() == currentTargetId) {
                int nextIndex = (i + 1) % candidates.size();
                currentTargetId = candidates.get(nextIndex).getId();
                System.out.println("Locked onto next: " + candidates.get(nextIndex).getName().getString());
                return;
            }
        }
        
        // Current target not in list, select first
        currentTargetId = candidates.get(0).getId();
        System.out.println("Target not found, locked onto first: " + candidates.get(0).getName().getString());
    }
    
    private static void targetPrevious(ClientWorld world, PlayerEntity player) {
        List<Entity> candidates = getTargetableCandidates(world, player);
        if (candidates.isEmpty()) return;
        
        // Sort by distance for consistent ordering
        candidates.sort((a, b) -> Double.compare(
            a.getPos().distanceTo(player.getPos()),
            b.getPos().distanceTo(player.getPos())
        ));
        
        if (currentTargetId == -1) {
            // No current target, select last
            currentTargetId = candidates.get(candidates.size() - 1).getId();
            System.out.println("Locked onto last: " + candidates.get(candidates.size() - 1).getName().getString());
            return;
        }
        
        // Find current target index and select previous
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).getId() == currentTargetId) {
                int prevIndex = (i - 1 + candidates.size()) % candidates.size();
                currentTargetId = candidates.get(prevIndex).getId();
                System.out.println("Locked onto previous: " + candidates.get(prevIndex).getName().getString());
                return;
            }
        }
        
        // Current target not in list, select last
        currentTargetId = candidates.get(candidates.size() - 1).getId();
        System.out.println("Target not found, locked onto last: " + candidates.get(candidates.size() - 1).getName().getString());
    }
    
    private static List<Entity> getTargetableCandidates(ClientWorld world, PlayerEntity player) {
        Vec3d playerPos = player.getPos();
        Box searchBox = new Box(playerPos.subtract(32, 32, 32), playerPos.add(32, 32, 32));
        
        return world.getOtherEntities(player, searchBox, entity -> {
            if (!(entity instanceof LivingEntity)) return false;
            
            // If no specific entity type is set, target all living entities
            if (entityTypeKey.isEmpty()) return true;
            
            // Filter by entity type key
            String entityName = entity.getType().toString().toLowerCase();
            String displayName = entity.getName().getString().toLowerCase();
            
            return entityName.contains(entityTypeKey) || 
                   displayName.contains(entityTypeKey) ||
                   matchesEntityCategory(entity, entityTypeKey);
        });
    }
    
    private static boolean matchesEntityCategory(Entity entity, String category) {
        return switch (category) {
            case "hostile", "mob", "monster" -> entity instanceof HostileEntity;
            case "passive", "animal" -> entity instanceof PassiveEntity;
            case "player" -> entity instanceof PlayerEntity;
            case "living" -> entity instanceof LivingEntity;
            default -> false;
        };
    }
}