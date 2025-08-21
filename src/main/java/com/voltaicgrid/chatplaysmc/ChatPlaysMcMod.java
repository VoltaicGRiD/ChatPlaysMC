package com.voltaicgrid.chatplaysmc;

import net.fabricmc.api.ModInitializer;
import com.voltaicgrid.chatplaysmc.config.ServerConfigManager;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatPlaysMcMod implements ModInitializer {
    public static final String MOD_ID = "chat_plays_mc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ServerConfigManager CONFIG = ServerConfigManager.createAndLoad();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Chat Plays MC mod");
        
        // Register entities
        ModEntities.register();
        
        // Register items
        ModItems.register();

        LOGGER.info("Chat Plays MC mod initialized successfully");
    }
}