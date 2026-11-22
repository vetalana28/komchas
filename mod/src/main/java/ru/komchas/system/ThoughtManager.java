package ru.komchas.system;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.komchas.data.Thoughts;
import ru.komchas.state.KomState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Мысли героинь: раз в 45–120 секунд игрок получает реплику в строке над хотбаром.
 * Чем выше Метка и день, тем чаще среди них попадаются «заражённые».
 */
public final class ThoughtManager {
    private static final Random RANDOM = new Random();
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private static boolean forceNow = false;

    /** Команда /komchas thought: выдать мысль всем сразу, не дожидаясь таймера. */
    public static void reset() {
        NEXT.clear();
        forceNow = true;
    }

    public static void tick(MinecraftServer server, KomState st, long gameTime, int dayTick) {
        if (forceNow) {
            forceNow = false;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                think(p, st, dayTick);
            }
            return;
        }
        if (gameTime % 40 != 0) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            long next = NEXT.getOrDefault(p.getUUID(), 0L);
            if (next == 0L) {
                NEXT.put(p.getUUID(), gameTime + 20L * (30 + RANDOM.nextInt(60)));
                continue;
            }
            if (gameTime < next) {
                continue;
            }
            think(p, st, dayTick);
            NEXT.put(p.getUUID(), gameTime + 20L * (45 + RANDOM.nextInt(75)));
        }
    }

    public static void think(ServerPlayer p, KomState st, int dayTick) {
        List<Thoughts.Thought> normal = new ArrayList<>();
        List<Thoughts.Thought> infected = new ArrayList<>();
        for (Thoughts.Thought t : Thoughts.ALL) {
            if (!eligible(t, p, st, dayTick)) {
                continue;
            }
            (t.infected() ? infected : normal).add(t);
        }
        double chance = Math.min(0.6, st.getMark(p.getUUID()) / 150.0 + (st.day - 1) * 0.02);
        List<Thoughts.Thought> pool = (!infected.isEmpty() && RANDOM.nextDouble() < chance) ? infected : normal;
        if (pool.isEmpty()) {
            return;
        }
        Thoughts.Thought chosen = pick(pool);
        Msg.actionBar(p, Component.literal(chosen.text()).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }

    private static boolean eligible(Thoughts.Thought t, ServerPlayer p, KomState st, int dayTick) {
        if (st.day < t.minDay() || st.day > t.maxDay()) {
            return false;
        }
        boolean night = dayTick >= 13000 && dayTick < 23000;
        if (t.time().equals("night") && !night) {
            return false;
        }
        if (t.time().equals("day") && night) {
            return false;
        }
        if (t.hungry() && p.getFoodData().getFoodLevel() > 8) {
            return false;
        }
        if (t.outdoors() && !p.level().canSeeSky(p.blockPosition())) {
            return false;
        }
        int mark = st.getMark(p.getUUID());
        if (mark < t.minMark() || mark > t.maxMark()) {
            return false;
        }
        String role = st.roles.get(p.getUUID());
        return t.who().equals("any") || role == null || role.equals(t.who());
    }

    private static Thoughts.Thought pick(List<Thoughts.Thought> pool) {
        int total = 0;
        for (Thoughts.Thought t : pool) {
            total += t.weight();
        }
        int roll = RANDOM.nextInt(total);
        for (Thoughts.Thought t : pool) {
            roll -= t.weight();
            if (roll < 0) {
                return t;
            }
        }
        return pool.get(0);
    }

    private ThoughtManager() {}
}
