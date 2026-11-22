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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Диалоги из data/komchas/dialogues/*.json. */
public final class Dialogues {
    /** kind: say (реплика), think (мысль), narrate (ремарка), sound (id звука), siren (секунды). */
    public record Line(String speaker, String text, String kind, int delay) {}

    public record Option(String label, int mark, int trust, String next) {}

    /** day 0 = любой день. from/to: тики суток, 0 = 6:00 утра. zone пустая = без привязки к месту. */
    public record Trigger(int day, int from, int to, String zone) {}

    public record Dialogue(String id, Trigger trigger, boolean once, List<Line> lines, List<Option> choices) {}

    public static final Map<String, Dialogue> ALL = new LinkedHashMap<>();

    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(new Gson(), "dialogues");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
            ALL.clear();
            for (Map.Entry<ResourceLocation, JsonElement> e : map.entrySet()) {
                if (!e.getKey().getNamespace().equals(KomChas.MODID)) {
                    continue;
                }
                try {
                    Dialogue d = parse(e.getKey().getPath(), e.getValue().getAsJsonObject());
                    ALL.put(d.id(), d);
                } catch (Exception ex) {
                    KomChas.LOGGER.error("Диалог {} не загружен: {}", e.getKey(), ex.toString());
                }
            }
            KomChas.LOGGER.info("Загружено диалогов: {}", ALL.size());
        }
    }

    static Dialogue parse(String id, JsonObject o) {
        Trigger trigger = null;
        if (o.has("trigger")) {
            JsonObject t = GsonHelper.getAsJsonObject(o, "trigger");
            trigger = new Trigger(
                    GsonHelper.getAsInt(t, "day", 0),
                    GsonHelper.getAsInt(t, "from", 0),
                    GsonHelper.getAsInt(t, "to", 23999),
                    GsonHelper.getAsString(t, "zone", ""));
        }
        List<Line> lines = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(o, "lines", new JsonArray())) {
            JsonObject l = el.getAsJsonObject();
            String text = GsonHelper.getAsString(l, "text");
            String kind = GsonHelper.getAsString(l, "kind", "say");
            lines.add(new Line(
                    GsonHelper.getAsString(l, "speaker", ""),
                    text,
                    kind,
                    GsonHelper.getAsInt(l, "delay", autoDelay(text))));
        }
        List<Option> choices = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(o, "choices", new JsonArray())) {
            JsonObject c = el.getAsJsonObject();
            choices.add(new Option(
                    GsonHelper.getAsString(c, "label"),
                    GsonHelper.getAsInt(c, "mark", 0),
                    GsonHelper.getAsInt(c, "trust", 0),
                    GsonHelper.getAsString(c, "next", "")));
        }
        return new Dialogue(id, trigger, GsonHelper.getAsBoolean(o, "once", true), lines, choices);
    }

    /** Время на прочтение: примерно 2 тика на символ, не меньше 2 и не больше 8 секунд. */
    private static int autoDelay(String text) {
        return Math.max(40, Math.min(160, 30 + text.length() * 2));
    }

    private Dialogues() {}
}
