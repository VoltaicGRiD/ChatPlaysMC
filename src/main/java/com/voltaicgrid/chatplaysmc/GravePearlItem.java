package com.voltaicgrid.chatplaysmc;

import java.util.EnumSet;

import com.voltaicgrid.chatplaysmc.data.LastDeathState;

import net.minecraft.item.Item;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.BlockPos;

public class GravePearlItem extends Item {
    public GravePearlItem(Settings settings) {
        super(settings);
    }

    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (world.isClient) return ActionResult.PASS;

        var server = world.getServer();
        var e = LastDeathState.get(server).get(player.getUuid());
        if (e == null) {
            player.sendMessage(Text.of("No death location found."), true);
            return ActionResult.SUCCESS;
        }

        ServerWorld targetWorld = server.getWorld(e.world());
        if (targetWorld == null) {
            player.sendMessage(Text.of("That world is unavailable."), true);
            return ActionResult.SUCCESS;
        }

        BlockPos target = e.pos();
        boolean safe = targetWorld.isInBuildLimit(target)
                    && targetWorld.isAir(target)
                    && targetWorld.isAir(target.up());
        if (!safe) {
            player.sendMessage(Text.of("Death spot is obstructed."), true);
            return ActionResult.SUCCESS;
        }

        double tx = target.getX() + 0.5;
        double ty = target.getY();
        double tz = target.getZ() + 0.5;

        EnumSet<PositionFlag> flags = EnumSet.noneOf(PositionFlag.class); // absolute
        ((ServerPlayerEntity) player).teleport(
            targetWorld, tx, ty, tz,
            flags,
            player.getYaw(), player.getPitch(),
            false
        );
        
        // Give the player 30 seconds of invulnerability and invisibility
        player.setInvulnerable(true);
        player.setInvisible(true);
        
        // Schedule removal of effects after 15 seconds (300 ticks)
        server.execute(() -> {
            // Use a simple counter-based approach with the server's tick scheduler
            new Thread(() -> {
                try {
                    Thread.sleep(15000); // 15 seconds
                    server.execute(() -> {
                        if (player instanceof ServerPlayerEntity serverPlayer && !serverPlayer.isDisconnected()) {
                            serverPlayer.setInvulnerable(false);
                            serverPlayer.setInvisible(false);
                            serverPlayer.sendMessage(Text.of("Protection effects have worn off."), true);
                        }
                    });
                } catch (InterruptedException exc) {
                	server.execute(() -> {
                        if (player instanceof ServerPlayerEntity serverPlayer && !serverPlayer.isDisconnected()) {
		                	serverPlayer.setInvulnerable(false);
		                    serverPlayer.setInvisible(false);
		                    serverPlayer.sendMessage(Text.of("Protection effects have worn off."), true);
                        }
                	});
                }
            }).start();
            
//            new Thread(() -> {
//            	try {
//            		while (true) {
//            			Thread.sleep(200); // 0.2 seconds
//            			server.execute(() -> {
//            				ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
//	            			if (serverPlayer.getLastAttackTime() > 0) {
//	            				serverPlayer.setInvulnerable(false);
//	            				serverPlayer.setInvisible(false);
//	            				serverPlayer.sendMessage(Text.of("Protection effects have worn off due to damage."), true);
//								break;
//							}
//            			});
//            		}
//            	} catch (InterruptedException exc) {
//            		serverPlayer.setInvulnerable(false);
//					serverPlayer.setInvisible(false);
//					serverPlayer.sendMessage(Text.of("Protection effects have worn off."), true);
//				}
//            });
        });
        
        // Remove the grave pearl from the player's inventory
        if (!player.getAbilities().creativeMode) {
			player.getStackInHand(hand).decrement(1);
		}
        
        return ActionResult.SUCCESS;
    }
}