package ru.komchas.system;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import ru.komchas.KomChas;
import ru.komchas.state.KomState;
import ru.komchas.state.MapData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Устанавливает готовую карту в любой мир при его первом запуске с этим модом.
 * Карта хранится внутри мода (папка data/komchas/map/region) в виде файлов регионов Minecraft.
 * Установка идёт ДО загрузки мира: файлы кладутся в папку мира, и игра читает их как свои.
 */
public final class MapInstaller {
    private static final String[] REGIONS = {"r.-1.-1.mca", "r.0.-1.mca", "r.-1.0.mca", "r.0.0.mca"};
    private static final String MARKER = "komchas_map.txt";

    /** Вызывается до загрузки мира. */
    public static void installFiles(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        Path marker = root.resolve(MARKER);
        if (Files.exists(marker)) {
            return;
        }
        try {
            Path region = root.resolve("region");
            Files.createDirectories(region);
            int copied = 0;
            for (String name : REGIONS) {
                try (InputStream in = MapInstaller.class.getResourceAsStream("/data/komchas/map/region/" + name)) {
                    if (in == null) {
                        KomChas.LOGGER.error("В моде нет файла карты {}", name);
                        continue;
                    }
                    Files.copy(in, region.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                    copied++;
                }
                // старые сущности и точки интереса этих регионов (в том числе лишние Тихие) убираем
                Files.deleteIfExists(root.resolve("entities").resolve(name));
                Files.deleteIfExists(root.resolve("poi").resolve(name));
            }
            if (copied == REGIONS.length) {
                Files.writeString(marker, "Карта Комендантского часа установлена модом.\n");
                KomChas.LOGGER.info("Карта установлена в {}", root);
            }
        } catch (IOException e) {
            KomChas.LOGGER.error("Не удалось установить карту: {}", e.toString());
        }
    }

    /** Вызывается после запуска мира: точка появления, правила, сброс состояния. */
    public static void setupWorld(MinecraftServer server) {
        KomState st = KomState.get(server);
        if (st.mapReady) {
            return;
        }
        MapData.ensureLoaded();
        // чистый старт: день 1, ничего не сыграно, Тихие не расставлены
        st.day = 1;
        st.initialized = false;
        st.played.clear();
        st.spawned.clear();
        st.moved.clear();
        st.zones.clear();
        MapData.applyZones(st);
        st.mapReady = true;
        st.setDirty();

        ServerLevel level = server.overworld();
        level.setDayTime(0L);
        int[] sp = MapData.SPAWN;
        level.setDefaultSpawnPos(new BlockPos(sp[0], sp[1], sp[2]), 0.0F);

        GameRules rules = server.getGameRules();
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        rules.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);
        rules.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
        rules.getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
        KomChas.LOGGER.info("Мир подготовлен: спавн {}, {}, {}", sp[0], sp[1], sp[2]);
    }

    /** Один раз переносит игрока в квартиру (если он уже играл в этом мире до установки карты). */
    public static void moveToFlat(MinecraftServer server, ServerPlayer player, KomState st) {
        if (!st.mapReady || st.moved.contains(player.getUUID())) {
            return;
        }
        st.moved.add(player.getUUID());
        st.setDirty();
        int[] sp = MapData.SPAWN;
        player.teleportTo(server.overworld(), sp[0] + 0.5, sp[1], sp[2] + 0.5, 0.0F, 0.0F);
    }

    private MapInstaller() {}
}
