package com.voltaicgrid.chatplaysmc.config;

import blue.endless.jankson.Jankson;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.ConfigWrapper.BuilderConsumer;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.util.Observable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ConfigManager extends ConfigWrapper<com.voltaicgrid.chatplaysmc.config.ConfigManagerModel> {

    public final Keys keys = new Keys();

    private final Option<java.lang.String> twitchChannel = this.optionForKey(this.keys.twitchChannel);
    private final Option<java.lang.Boolean> useOldTowering = this.optionForKey(this.keys.useOldTowering);
    private final Option<java.lang.Boolean> showHudInformation = this.optionForKey(this.keys.showHudInformation);
    private final Option<java.lang.Boolean> enableLockOn = this.optionForKey(this.keys.enableLockOn);
    private final Option<java.lang.Boolean> enableSwimCommands = this.optionForKey(this.keys.enableSwimCommands);
    private final Option<java.lang.Boolean> allowButtonClickingOnPauseMenus = this.optionForKey(this.keys.allowButtonClickingOnPauseMenus);
    private final Option<java.lang.Boolean> allowButtonClickingOnMainMenus = this.optionForKey(this.keys.allowButtonClickingOnMainMenus);

    private ConfigManager() {
        super(com.voltaicgrid.chatplaysmc.config.ConfigManagerModel.class);
    }

    private ConfigManager(BuilderConsumer consumer) {
        super(com.voltaicgrid.chatplaysmc.config.ConfigManagerModel.class, consumer);
    }

    public static ConfigManager createAndLoad() {
        var wrapper = new ConfigManager();
        wrapper.load();
        return wrapper;
    }

    public static ConfigManager createAndLoad(BuilderConsumer consumer) {
        var wrapper = new ConfigManager(consumer);
        wrapper.load();
        return wrapper;
    }

    public java.lang.String twitchChannel() {
        return twitchChannel.value();
    }

    public void twitchChannel(java.lang.String value) {
        twitchChannel.set(value);
    }

    public boolean useOldTowering() {
        return useOldTowering.value();
    }

    public void useOldTowering(boolean value) {
        useOldTowering.set(value);
    }

    public boolean showHudInformation() {
        return showHudInformation.value();
    }

    public void showHudInformation(boolean value) {
        showHudInformation.set(value);
    }

    public boolean enableLockOn() {
        return enableLockOn.value();
    }

    public void enableLockOn(boolean value) {
        enableLockOn.set(value);
    }

    public boolean enableSwimCommands() {
        return enableSwimCommands.value();
    }

    public void enableSwimCommands(boolean value) {
        enableSwimCommands.set(value);
    }

    public boolean allowButtonClickingOnPauseMenus() {
        return allowButtonClickingOnPauseMenus.value();
    }

    public void allowButtonClickingOnPauseMenus(boolean value) {
        allowButtonClickingOnPauseMenus.set(value);
    }

    public boolean allowButtonClickingOnMainMenus() {
        return allowButtonClickingOnMainMenus.value();
    }

    public void allowButtonClickingOnMainMenus(boolean value) {
        allowButtonClickingOnMainMenus.set(value);
    }


    public static class Keys {
        public final Option.Key twitchChannel = new Option.Key("twitchChannel");
        public final Option.Key useOldTowering = new Option.Key("useOldTowering");
        public final Option.Key showHudInformation = new Option.Key("showHudInformation");
        public final Option.Key enableLockOn = new Option.Key("enableLockOn");
        public final Option.Key enableSwimCommands = new Option.Key("enableSwimCommands");
        public final Option.Key allowButtonClickingOnPauseMenus = new Option.Key("allowButtonClickingOnPauseMenus");
        public final Option.Key allowButtonClickingOnMainMenus = new Option.Key("allowButtonClickingOnMainMenus");
    }
}

