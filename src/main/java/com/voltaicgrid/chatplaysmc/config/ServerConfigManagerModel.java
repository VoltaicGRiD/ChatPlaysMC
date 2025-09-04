package com.voltaicgrid.chatplaysmc.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.SectionHeader;

@Modmenu(modId = "chat_plays_mc-server")
@Config(name = "chat_plays_mc-server", wrapperName = "ServerConfigManager")
public class ServerConfigManagerModel {
	@SectionHeader("Custom Items")
	public boolean enableOrio = true;
	public boolean enableSoap = true;
	public boolean enableFoodSatchel = true;
	public boolean enableTorchSatchel = true;
	public int torchSatchelLightThreshold = 7;
	public boolean enablePlohtArmor = true;
	public boolean enableShaftBuilder = true;
	public int shaftBuilderDepth = 48;
	public boolean enableUppies = true;
}