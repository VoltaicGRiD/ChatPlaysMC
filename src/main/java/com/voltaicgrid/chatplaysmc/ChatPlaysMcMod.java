package com.voltaicgrid.chatplaysmc;

import com.voltaicgrid.chatplaysmc.config.ServerConfigManager;
import com.voltaicgrid.chatplaysmc.data.LastDeathState;

import net.fabricmc.api.ModInitializer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatPlaysMcMod implements ModInitializer {
    public static final String MOD_ID = "chat_plays_mc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ServerConfigManager CONFIG = ServerConfigManager.createAndLoad();
    public static final TickScheduler SCHEDULER = new TickScheduler();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Chat Plays MC mod");
        
        // Register entities
        ModEntities.init();
        
        // Register items
        ModItems.register();
        
        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, net.minecraft.entity.damage.DamageSource source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                var server = player.getServer();
                if (server == null) return;

                LastDeathState.get(server).set(
                    player.getUuid(),
                    player.getWorld().getRegistryKey(),
                    player.getBlockPos(),
                    player.getYaw(),
                    player.getPitch()
                );
            }
        });
        
        ServerTickEvents.END_SERVER_TICK.register(server -> SCHEDULER.tick());

        LOGGER.info("Chat Plays MC mod initialized successfully");
    }
}