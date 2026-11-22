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

/** Мысли героинь из data/komchas/thoughts/*.json. */
public final class Thoughts {
    /**
     * who: veta / sonya / any. time: day / night / any.
     * infected: «заражённая» мысль, выглядит как обычная, но не принадлежит героине.
     */
    public record Thought(String who, String text, int minDay, int maxDay, String time,
                          boolean hungry, boolean outdoors, int minMark, int maxMark,
                          boolean infected, int weight) {}

    public static final List<Thought> ALL = new ArrayList<>();

    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(new Gson(), "thoughts");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
            ALL.clear();
            for (Map.Entry<ResourceLocation, JsonElement> e : map.entrySet()) {
                if (!e.getKey().getNamespace().equals(KomChas.MODID)) {
                    continue;
                }
                try {
                    JsonArray arr = GsonHelper.getAsJsonArray(e.getValue().getAsJsonObject(), "thoughts", new JsonArray());
                    for (JsonElement el : arr) {
                        JsonObject o = el.getAsJsonObject();
                        ALL.add(new Thought(
                                GsonHelper.getAsString(o, "who", "any"),
                                GsonHelper.getAsString(o, "text"),
                                GsonHelper.getAsInt(o, "min_day", 1),
                                GsonHelper.getAsInt(o, "max_day", 12),
                                GsonHelper.getAsString(o, "time", "any"),
                                GsonHelper.getAsBoolean(o, "hungry", false),
                                GsonHelper.getAsBoolean(o, "outdoors", false),
                                GsonHelper.getAsInt(o, "min_mark", 0),
                                GsonHelper.getAsInt(o, "max_mark", 100),
                                GsonHelper.getAsBoolean(o, "infected", false),
                                Math.max(1, GsonHelper.getAsInt(o, "weight", 1))));
                    }
                } catch (Exception ex) {
                    KomChas.LOGGER.error("Мысли {} не загружены: {}", e.getKey(), ex.toString());
                }
            }
            KomChas.LOGGER.info("Загружено мыслей: {}", ALL.size());
        }
    }

    private Thoughts() {}
}
