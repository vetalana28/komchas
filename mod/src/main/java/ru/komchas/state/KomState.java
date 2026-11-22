package ru.komchas.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Всё, что должно переживать перезапуск мира. */
public class KomState extends SavedData {
    public static final int MAX_DAY = 12;

    public boolean initialized = false;
    public long lastDayIndex = 0;
    public int day = 1;
    /** Доверие между героинями, 0..100. */
    public int trust = 50;

    public final Map<UUID, String> roles = new HashMap<>();   // "veta" или "sonya"
    public final Map<UUID, Integer> mark = new HashMap<>();   // Метка, 0..100
    public final Map<UUID, Integer> skips = new HashMap<>();  // прогулы
    public final Set<String> played = new HashSet<>();        // уже сыгранные диалоги
    public final Map<String, Zone> zones = new HashMap<>();   // именованные зоны (flat, yard, college...)

    public record Zone(int x, int y, int z, int r) {}

    public static KomState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(KomState::load, KomState::new, "komchas_state");
    }

    public int getMark(UUID id) {
        return mark.getOrDefault(id, 0);
    }

    public void addMark(UUID id, int delta) {
        mark.put(id, Math.max(0, Math.min(100, getMark(id) + delta)));
        setDirty();
    }

    public void addSkip(UUID id) {
        skips.merge(id, 1, Integer::sum);
        setDirty();
    }

    public void addTrust(int delta) {
        trust = Math.max(0, Math.min(100, trust + delta));
        setDirty();
    }

    public UUID playerWithRole(String role) {
        for (Map.Entry<UUID, String> e : roles.entrySet()) {
            if (e.getValue().equals(role)) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("initialized", initialized);
        tag.putLong("lastDayIndex", lastDayIndex);
        tag.putInt("day", day);
        tag.putInt("trust", trust);
        tag.put("roles", writeMap(roles, (t, v) -> t.putString("v", v)));
        tag.put("mark", writeMap(mark, (t, v) -> t.putInt("v", v)));
        tag.put("skips", writeMap(skips, (t, v) -> t.putInt("v", v)));

        ListTag playedTag = new ListTag();
        for (String s : played) {
            playedTag.add(StringTag.valueOf(s));
        }
        tag.put("played", playedTag);

        ListTag zonesTag = new ListTag();
        for (Map.Entry<String, Zone> e : zones.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("name", e.getKey());
            t.putInt("x", e.getValue().x());
            t.putInt("y", e.getValue().y());
            t.putInt("z", e.getValue().z());
            t.putInt("r", e.getValue().r());
            zonesTag.add(t);
        }
        tag.put("zones", zonesTag);
        return tag;
    }

    public static KomState load(CompoundTag tag) {
        KomState s = new KomState();
        s.initialized = tag.getBoolean("initialized");
        s.lastDayIndex = tag.getLong("lastDayIndex");
        s.day = Math.max(1, Math.min(MAX_DAY, tag.getInt("day")));
        s.trust = tag.contains("trust") ? tag.getInt("trust") : 50;

        ListTag roles = tag.getList("roles", Tag.TAG_COMPOUND);
        for (int i = 0; i < roles.size(); i++) {
            CompoundTag t = roles.getCompound(i);
            s.roles.put(t.getUUID("id"), t.getString("v"));
        }
        ListTag mark = tag.getList("mark", Tag.TAG_COMPOUND);
        for (int i = 0; i < mark.size(); i++) {
            CompoundTag t = mark.getCompound(i);
            s.mark.put(t.getUUID("id"), t.getInt("v"));
        }
        ListTag skips = tag.getList("skips", Tag.TAG_COMPOUND);
        for (int i = 0; i < skips.size(); i++) {
            CompoundTag t = skips.getCompound(i);
            s.skips.put(t.getUUID("id"), t.getInt("v"));
        }
        ListTag played = tag.getList("played", Tag.TAG_STRING);
        for (int i = 0; i < played.size(); i++) {
            s.played.add(played.getString(i));
        }
        ListTag zones = tag.getList("zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < zones.size(); i++) {
            CompoundTag t = zones.getCompound(i);
            s.zones.put(t.getString("name"),
                    new Zone(t.getInt("x"), t.getInt("y"), t.getInt("z"), t.getInt("r")));
        }
        return s;
    }

    private static <V> ListTag writeMap(Map<UUID, V> map, BiConsumer<CompoundTag, V> writer) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, V> e : map.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", e.getKey());
            writer.accept(t, e.getValue());
            list.add(t);
        }
        return list;
    }
}
