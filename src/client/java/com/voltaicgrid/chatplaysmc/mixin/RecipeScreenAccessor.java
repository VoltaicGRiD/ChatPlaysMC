package com.voltaicgrid.chatplaysmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;

@Mixin(RecipeBookScreen.class)
public interface RecipeScreenAccessor {
    @Accessor("recipeBook")
    RecipeBookWidget getRecipeBook();
}