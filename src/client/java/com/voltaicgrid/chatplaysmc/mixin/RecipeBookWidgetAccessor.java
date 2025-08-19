package com.voltaicgrid.chatplaysmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.screen.recipebook.RecipeBookResults;
import net.minecraft.client.gui.widget.TextFieldWidget;

@Mixin(RecipeBookWidget.class)
public interface RecipeBookWidgetAccessor {
    @Accessor("recipesArea")
    RecipeBookResults getRecipesArea();

    @Accessor("searchField")
    TextFieldWidget getSearchField();
}