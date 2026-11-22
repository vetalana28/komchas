package ru.komchas.system;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import ru.komchas.ModEntities;
import ru.komchas.entity.TikhiyEntity;
import ru.komchas.state.KomState;
import ru.komchas.state.MapData;

import java.util.List;

/**
 * Расставляет Тихих по окнам дома напротив: с каждым днём их больше.
 * Работает только там, где чанк уже загружен, поэтому нагрузка небольшая.
 */
public final class TikhiySpawner {
    /** Сколько Тихих в окнах к дню 1..12. */
    private static final int[] WINDOW_COUNT = {1, 2, 2, 3, 5, 8, 12, 18, 24, 32, 40, 50};

    public static void tick(MinecraftServer server, KomState st, long gameTime) {
        if (gameTime % 100 != 0) {
            return;
        }
        MapData.ensureLoaded();
        ServerLevel level = server.overworld();
        int day = Math.max(1, Math.min(KomState.MAX_DAY, st.day));

        spawnFirst(level, MapData.WINDOWS, WINDOW_COUNT[day - 1]);
        if (day >= 5) {
            spawnFirst(level, MapData.SKI, 1);
        }
        if (day >= 9) {
            spawnFirst(level, MapData.HALL, Math.min(MapData.HALL.size(), (day - 8) * 20));
        }
    }

    private static void spawnFirst(ServerLevel level, List<MapData.Spot> spots, int count) {
        for (int i = 0; i < Math.min(count, spots.size()); i++) {
            MapData.Spot s = spots.get(i);
            BlockPos pos = new BlockPos(s.x(), s.y(), s.z());
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            AABB box = new AABB(pos).inflate(1.5);
            if (!level.getEntitiesOfClass(TikhiyEntity.class, box).isEmpty()) {
                continue;
            }
            TikhiyEntity e = ModEntities.TIKHIY.get().create(level);
            if (e == null) {
                continue;
            }
            e.moveTo(s.x() + 0.5, s.y(), s.z() + 0.5, s.yaw(), 0.0F);
            level.addFreshEntity(e);
        }
    }

    private TikhiySpawner() {}
}
