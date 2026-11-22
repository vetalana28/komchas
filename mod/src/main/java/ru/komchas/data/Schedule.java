package ru.komchas.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import ru.komchas.KomChas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Расписание и сирены из data/komchas/schedule/*.json. Время в тиках суток, 0 = 6:00. */
public final class Schedule {
    /** type: class (пара, прогул наказывается Меткой) или event (просто событие). */
    public record Event(int day, int from, int to, String title, String zone, String type) {}

    public record Siren(int day, int at, int duration) {}

    public static final List<Event> EVENTS = new ArrayList<>();
    public static final List<Siren> SIRENS = new ArrayList<>();

    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(new Gson(), "schedule");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
            EVENTS.clear();
            SIRENS.clear();
            for (Map.Entry<ResourceLocation, JsonElement> e : map.entrySet()) {
                if (!e.getKey().getNamespace().equals(KomChas.MODID)) {
                    continue;
                }
                try {
                    JsonObject root = e.getValue().getAsJsonObject();
                    for (JsonElement el : GsonHelper.getAsJsonArray(root, "events", new JsonArray())) {
                        JsonObject o = el.getAsJsonObject();
                        EVENTS.add(new Event(
                                GsonHelper.getAsInt(o, "day"),
                                GsonHelper.getAsInt(o, "from"),
                                GsonHelper.getAsInt(o, "to"),
                                GsonHelper.getAsString(o, "title"),
                                GsonHelper.getAsString(o, "zone", ""),
                                GsonHelper.getAsString(o, "type", "class")));
                    }
                    for (JsonElement el : GsonHelper.getAsJsonArray(root, "sirens", new JsonArray())) {
                        JsonObject o = el.getAsJsonObject();
                        SIRENS.add(new Siren(
                                GsonHelper.getAsInt(o, "day"),
                                GsonHelper.getAsInt(o, "at"),
                                GsonHelper.getAsInt(o, "duration", 40)));
                    }
                } catch (Exception ex) {
                    KomChas.LOGGER.error("Расписание {} не загружено: {}", e.getKey(), ex.toString());
                }
            }
            KomChas.LOGGER.info("Расписание: событий {}, сирен {}", EVENTS.size(), SIRENS.size());
        }
    }

    private Schedule() {}
}
