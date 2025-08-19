package com.voltaicgrid.chatplaysmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screen.recipebook.RecipeBookResults;
import net.minecraft.client.gui.screen.recipebook.AnimatedResultButton;
import java.util.List;

@Mixin(RecipeBookResults.class)
public interface RecipeBookResultsAccessor {
    @Accessor("resultButtons")
    List<AnimatedResultButton> getResultButtons();
}
