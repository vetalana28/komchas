package ru.komchas.state;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.komchas.KomChas;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Данные карты из data/komchas/map/zones.json: зоны, окна для Тихих, ниши зала. Файл создаёт генератор карты. */
public final class MapData {
    public record Spot(int x, int y, int z, float yaw) {}

    public static final List<Spot> WINDOWS = new ArrayList<>();
    public static final List<Spot> HALL = new ArrayList<>();
    public static final List<Spot> SKI = new ArrayList<>();
    /** Точка появления игроков (квартира девочек): x, y, z. */
    public static int[] SPAWN = {5, 80, -21};
    private static JsonObject zonesJson;
    private static boolean loaded = false;

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try (InputStream in = MapData.class.getResourceAsStream("/data/komchas/map/zones.json")) {
            if (in == null) {
                KomChas.LOGGER.warn("zones.json не найден: карта без готовых зон");
                return;
            }
            JsonObject root = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            zonesJson = root.getAsJsonObject("zones");
            if (root.has("spawn")) {
                JsonArray sp = root.getAsJsonArray("spawn");
                SPAWN = new int[]{sp.get(0).getAsInt(), sp.get(1).getAsInt(), sp.get(2).getAsInt()};
            }
            readSpots(root.getAsJsonArray("tikhiy_windows"), WINDOWS);
            readSpots(root.getAsJsonArray("tikhiy_hall"), HALL);
            readSpots(root.getAsJsonArray("tikhiy_ski"), SKI);
            KomChas.LOGGER.info("Карта: окон {}, ниш {}, зон {}", WINDOWS.size(), HALL.size(),
                    zonesJson == null ? 0 : zonesJson.size());
        } catch (Exception e) {
            KomChas.LOGGER.error("Не удалось прочитать zones.json: {}", e.toString());
        }
    }

    private static void readSpots(JsonArray arr, List<Spot> out) {
        if (arr == null) {
            return;
        }
        for (JsonElement el : arr) {
            JsonArray a = el.getAsJsonArray();
            out.add(new Spot(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsFloat()));
        }
    }

    /** Заполняет зоны по умолчанию, если игрок их ещё не задавал. */
    public static void applyZones(KomState st) {
        ensureLoaded();
        if (zonesJson == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : zonesJson.entrySet()) {
            if (st.zones.containsKey(e.getKey())) {
                continue;
            }
            JsonObject z = e.getValue().getAsJsonObject();
            st.zones.put(e.getKey(), new KomState.Zone(
                    z.get("x").getAsInt(), z.get("y").getAsInt(), z.get("z").getAsInt(), z.get("r").getAsInt()));
        }
        st.setDirty();
    }

    private MapData() {}
}
