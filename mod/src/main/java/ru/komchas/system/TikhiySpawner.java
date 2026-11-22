package ru.komchas.system;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntityTypeTest;
import ru.komchas.ModEntities;
import ru.komchas.entity.TikhiyEntity;
import ru.komchas.state.KomState;
import ru.komchas.state.MapData;

import java.util.List;

/**
 * Расставляет Тихих по окнам дома напротив: с каждым днём их больше.
 * Каждое место занимается ОДИН раз: номер места записывается в состояние мира.
 * Раньше мод проверял «есть ли тут уже Тихий» через поиск сущностей, а он не видит сущностей
 * в ещё не активных чанках, и мод создавал новых каждые пять секунд. Теперь поиска нет.
 */
public final class TikhiySpawner {
    /** Сколько Тихих в окнах к дню 1..12. */
    private static final int[] WINDOW_COUNT = {1, 2, 2, 3, 5, 8, 12, 18, 24, 32, 40, 50};
    /** Жёсткий предел на всякий случай. */
    private static final int HARD_CAP = 140;
    /** Не больше стольких новых Тихих за один проход. */
    private static final int PER_PASS = 3;

    public static void tick(MinecraftServer server, KomState st, long gameTime) {
        if (gameTime % 100 != 0 || !st.mapReady) {
            return;
        }
        MapData.ensureLoaded();
        ServerLevel level = server.overworld();
        int total = level.getEntities(EntityTypeTest.forClass(TikhiyEntity.class), e -> true).size();
        if (total >= HARD_CAP) {
            return;
        }
        int day = Math.max(1, Math.min(KomState.MAX_DAY, st.day));
        int budget = PER_PASS;
        budget -= spawnFirst(level, st, "w", MapData.WINDOWS, WINDOW_COUNT[day - 1], budget);
        if (day >= 5 && budget > 0) {
            budget -= spawnFirst(level, st, "s", MapData.SKI, 1, budget);
        }
        if (day >= 9 && budget > 0) {
            spawnFirst(level, st, "h", MapData.HALL, Math.min(MapData.HALL.size(), (day - 8) * 20), budget);
        }
    }

    /** Возвращает, сколько Тихих создано. */
    private static int spawnFirst(ServerLevel level, KomState st, String prefix, List<MapData.Spot> spots,
                                  int count, int budget) {
        int made = 0;
        for (int i = 0; i < Math.min(count, spots.size()) && made < budget; i++) {
            String key = prefix + i;
            if (st.spawned.contains(key)) {
                continue;
            }
            MapData.Spot s = spots.get(i);
            BlockPos pos = new BlockPos(s.x(), s.y(), s.z());
            if (!level.hasChunkAt(pos)) {
                continue; // чанк не загружен, вернёмся позже
            }
            TikhiyEntity e = ModEntities.TIKHIY.get().create(level);
            if (e == null) {
                continue;
            }
            e.moveTo(s.x() + 0.5, s.y(), s.z() + 0.5, s.yaw(), 0.0F);
            level.addFreshEntity(e);
            st.spawned.add(key);
            st.setDirty();
            made++;
        }
        return made;
    }

    private TikhiySpawner() {}
}
