package com.voltaicgrid.chatplaysmc.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.SectionHeader;
import io.wispforest.owo.config.annotation.Modmenu;

@Modmenu(modId = "chat_plays_mc")
@Config(name = "chat_plays_mc", wrapperName = "ConfigManager")
public class ConfigManagerModel {
	public String twitchChannel = "chatplaysmc";
	
	@SectionHeader("Mechanics")
	public boolean useOldTowering = false;
	public boolean useOldJumpMechanics = false;
	public boolean showHudInformation = true;
	public boolean enableLockOn = true;
	public boolean enableSwimCommands = true;
	
	@SectionHeader("Button Clicking")
	public boolean allowButtonClickingOnPauseMenus = false;
	public boolean allowButtonClickingOnMainMenus = false;
}