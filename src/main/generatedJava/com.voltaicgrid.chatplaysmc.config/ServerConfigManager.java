package com.voltaicgrid.chatplaysmc.config;

import blue.endless.jankson.Jankson;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.ConfigWrapper.BuilderConsumer;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.util.Observable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ServerConfigManager extends ConfigWrapper<com.voltaicgrid.chatplaysmc.config.ServerConfigManagerModel> {

    public final Keys keys = new Keys();

    private final Option<java.lang.Boolean> enableOrio = this.optionForKey(this.keys.enableOrio);
    private final Option<java.lang.Boolean> enableSoap = this.optionForKey(this.keys.enableSoap);
    private final Option<java.lang.Boolean> enableFoodSatchel = this.optionForKey(this.keys.enableFoodSatchel);
    private final Option<java.lang.Boolean> enableTorchSatchel = this.optionForKey(this.keys.enableTorchSatchel);
    private final Option<java.lang.Integer> torchSatchelLightThreshold = this.optionForKey(this.keys.torchSatchelLightThreshold);
    private final Option<java.lang.Boolean> enablePlohtArmor = this.optionForKey(this.keys.enablePlohtArmor);
    private final Option<java.lang.Boolean> enableShaftBuilder = this.optionForKey(this.keys.enableShaftBuilder);
    private final Option<java.lang.Integer> shaftBuilderDepth = this.optionForKey(this.keys.shaftBuilderDepth);

    private ServerConfigManager() {
        super(com.voltaicgrid.chatplaysmc.config.ServerConfigManagerModel.class);
    }

    private ServerConfigManager(BuilderConsumer consumer) {
        super(com.voltaicgrid.chatplaysmc.config.ServerConfigManagerModel.class, consumer);
    }

    public static ServerConfigManager createAndLoad() {
        var wrapper = new ServerConfigManager();
        wrapper.load();
        return wrapper;
    }

    public static ServerConfigManager createAndLoad(BuilderConsumer consumer) {
        var wrapper = new ServerConfigManager(consumer);
        wrapper.load();
        return wrapper;
    }

    public boolean enableOrio() {
        return enableOrio.value();
    }

    public void enableOrio(boolean value) {
        enableOrio.set(value);
    }

    public boolean enableSoap() {
        return enableSoap.value();
    }

    public void enableSoap(boolean value) {
        enableSoap.set(value);
    }

    public boolean enableFoodSatchel() {
        return enableFoodSatchel.value();
    }

    public void enableFoodSatchel(boolean value) {
        enableFoodSatchel.set(value);
    }

    public boolean enableTorchSatchel() {
        return enableTorchSatchel.value();
    }

    public void enableTorchSatchel(boolean value) {
        enableTorchSatchel.set(value);
    }

    public int torchSatchelLightThreshold() {
        return torchSatchelLightThreshold.value();
    }

    public void torchSatchelLightThreshold(int value) {
        torchSatchelLightThreshold.set(value);
    }

    public boolean enablePlohtArmor() {
        return enablePlohtArmor.value();
    }

    public void enablePlohtArmor(boolean value) {
        enablePlohtArmor.set(value);
    }

    public boolean enableShaftBuilder() {
        return enableShaftBuilder.value();
    }

    public void enableShaftBuilder(boolean value) {
        enableShaftBuilder.set(value);
    }

    public int shaftBuilderDepth() {
        return shaftBuilderDepth.value();
    }

    public void shaftBuilderDepth(int value) {
        shaftBuilderDepth.set(value);
    }


    public static class Keys {
        public final Option.Key enableOrio = new Option.Key("enableOrio");
        public final Option.Key enableSoap = new Option.Key("enableSoap");
        public final Option.Key enableFoodSatchel = new Option.Key("enableFoodSatchel");
        public final Option.Key enableTorchSatchel = new Option.Key("enableTorchSatchel");
        public final Option.Key torchSatchelLightThreshold = new Option.Key("torchSatchelLightThreshold");
        public final Option.Key enablePlohtArmor = new Option.Key("enablePlohtArmor");
        public final Option.Key enableShaftBuilder = new Option.Key("enableShaftBuilder");
        public final Option.Key shaftBuilderDepth = new Option.Key("shaftBuilderDepth");
    }
}

