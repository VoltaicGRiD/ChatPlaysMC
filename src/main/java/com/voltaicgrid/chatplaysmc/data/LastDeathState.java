package com.voltaicgrid.chatplaysmc.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtOps;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.world.PersistentStateType;

import java.util.Map;
import java.util.UUID;

public class LastDeathState extends PersistentState {
    private static final String SAVE_ID = "chatplaysmc_last_deaths";

    public static record Entry(RegistryKey<World> world, BlockPos pos, float yaw, float pitch) {}

    private final Map<UUID, Entry> entries = new Object2ObjectOpenHashMap<>();

    public void set(UUID uuid, RegistryKey<World> world, BlockPos pos, float yaw, float pitch) {
        entries.put(uuid, new Entry(world, pos.toImmutable(), yaw, pitch));
        markDirty();
    }

    public Entry get(UUID uuid) {
        return entries.get(uuid);
    }

    // ---------- NBT ----------

    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (var e : entries.entrySet()) {
            UUID uuid = e.getKey();
            Entry v   = e.getValue();

            NbtCompound tag = new NbtCompound();
            tag.putString("uuid", uuid.toString());
            tag.putString("world", v.world().getValue().toString());
            tag.putInt("x", v.pos().getX());
            tag.putInt("y", v.pos().getY());
            tag.putInt("z", v.pos().getZ());
            tag.putFloat("yaw", v.yaw());
            tag.putFloat("pitch", v.pitch());
            list.add(tag);
        }
        nbt.put("entries", list);
        return nbt;
    }

    public static LastDeathState fromNbt(NbtCompound nbt) {
        LastDeathState state = new LastDeathState();

        NbtElement el = nbt.get("entries");
        if (!(el instanceof NbtList list)) return state;

        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompound(i).orElse(null);
            String uuidStr  = tag.getString("uuid").orElse(null);
            String worldStr = tag.getString("world").orElse(null);
            if (uuidStr == null || worldStr == null) continue;

            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (Exception ex) { continue; }

            Identifier wid = Identifier.tryParse(worldStr);
            if (wid == null) continue;

            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, wid);
            BlockPos pos = new BlockPos(tag.getInt("x").orElse(0), tag.getInt("y").orElse(0), tag.getInt("z").orElse(0));
            float yaw = tag.getFloat("yaw").orElse(0.0f);
            float pitch = tag.getFloat("pitch").orElse(0.0f);

            state.entries.put(uuid, new Entry(worldKey, pos, yaw, pitch));
        }
        return state;
    }

    // ---------- Access ----------

    private static final PersistentStateType<LastDeathState> TYPE = new PersistentStateType<>(
        SAVE_ID,
        context -> new LastDeathState(),
        context -> Codec.unit(new LastDeathState()),
        DataFixTypes.LEVEL
    );

    public static LastDeathState get(MinecraftServer server) {
        PersistentStateManager psm = server.getOverworld().getPersistentStateManager();
        return psm.getOrCreate(TYPE);
    }
}