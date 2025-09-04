package com.voltaicgrid.chatplaysmc;

import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModTags {
    public static final TagKey<Item> PAXEL = createTag("paxel");

    private static TagKey<Item> createTag(String name) {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, name));
    }
}
