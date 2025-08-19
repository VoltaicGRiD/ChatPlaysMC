package com.voltaicgrid.chatplaysmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;

@Mixin(RecipeBookWidget.class)
public interface RecipeBookWidgetInvoker {
    // Try method_2659 which is often the obfuscated name for refresh methods
    @Invoker("refreshResults")
    void invokeRefreshResults(boolean resetCurrentPage, boolean filteringCraftable);
}