package com.voltaicgrid.chatplaysmc;

import java.util.EnumSet;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UppiesItem extends Item {
    public UppiesItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        // Do nothing on the client; server will perform the action and sync.
        if (world.isClient) {
            return ActionResult.PASS;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        BlockPos start = serverPlayer.getBlockPos();
        BlockPos.Mutable pos = new BlockPos.Mutable(start.getX(), start.getY(), start.getZ());
        BlockPos lastSolid = start;
        int airCount = 0;

        // Prefer a world-defined ceiling if available; otherwise cap at build height.
        final int topY = 340;

        for (int y = start.getY(); y < topY; y++) {
            pos.setY(y);

            if (serverWorld.isAir(pos)) {
                airCount++;
            } else {
                lastSolid = pos.toImmutable();
                airCount = 0;
            }

            // Once we've found enough headroom, attempt the teleport just above the last solid.
            if (airCount > 20) {
                BlockPos target = lastSolid.up();

                // Basic safety checks
                boolean canStand = serverWorld.isAir(target) && serverWorld.isAir(target.up());
                boolean inWorld  = serverWorld.isInBuildLimit(target);

                if (inWorld && canStand) {
                    double tx = target.getX() + 0.5;
                    double ty = target.getY();
                    double tz = target.getZ() + 0.5;

                    EnumSet<PositionFlag> flags = EnumSet.noneOf(PositionFlag.class); // absolute coords & rotation
                    serverPlayer.teleport(serverWorld, tx, ty, tz, flags, serverPlayer.getYaw(), serverPlayer.getPitch(), false);

                    // Remove the item from the player's hand
                    if (hand == Hand.MAIN_HAND) {
						serverPlayer.getMainHandStack().decrement(1);
					} else {
						serverPlayer.getOffHandStack().decrement(1);
					}
                    
                    return ActionResult.SUCCESS;
                }
            }
        }

        return ActionResult.PASS;
    }
}
