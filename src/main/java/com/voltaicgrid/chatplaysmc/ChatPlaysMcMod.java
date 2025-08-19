package com.voltaicgrid.chatplaysmc;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

import com.github.philippheuer.events4j.core.EventManager;
import com.github.twitch4j.*;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

public class ChatPlaysMcMod implements ModInitializer {
    public static final String MOD_ID = "chat_plays_mc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static TwitchClient twitchClient;
    public static EventManager eventManager;

    public static final Map<String, String> OBJECTIVES = Map.of(
        "objective1", "Description for objective 1",
        "objective2", "Description for objective 2"
    );

    @Override
    public void onInitialize() {
        LOGGER.info("{} initialized!", MOD_ID);
        
        ModItems.initialize();
    }

}