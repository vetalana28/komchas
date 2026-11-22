package ru.komchas.system;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import ru.komchas.ModSounds;
import ru.komchas.data.Schedule;
import ru.komchas.state.KomState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сирена: звучит заданное время, потом наступает «тихий час».
 * Пока тихий час идёт, игрок на открытом небе набирает Метку.
 */
public final class SirenManager {
    private static final int QUIET_SECONDS = 90;
    private static final int LAUGH_DELAY_TICKS = 400;

    private static boolean running = false;
    private static long endsAt = -1;
    private static long nextHorn = 0;
    private static long quietUntil = -1;
    private static long laughAt = -1;

    private static final Set<String> fired = new HashSet<>();
    private static final List<int[]> windows = new ArrayList<>(); // окна сирен сегодня, в тиках суток

    public static void onNewDay() {
        fired.clear();
        windows.clear();
    }

    public static boolean overlaps(int from, int to) {
        for (int[] w : windows) {
            if (w[0] < to && w[1] > from) {
                return true;
            }
        }
        return false;
    }

    public static void start(MinecraftServer server, long gameTime, int dayTick, int seconds) {
        running = true;
        endsAt = gameTime + seconds * 20L;
        nextHorn = gameTime;
        quietUntil = endsAt + QUIET_SECONDS * 20L;
        windows.add(new int[]{dayTick, dayTick + seconds * 20});
        Msg.title(server,
                Component.literal("СИГНАЛ ОПОВЕЩЕНИЯ").withStyle(ChatFormatting.RED),
                Component.literal("Учебная тревога. Оставайтесь внутри.").withStyle(ChatFormatting.GRAY));
    }

    public static void tick(MinecraftServer server, KomState st, long gameTime, int dayTick) {
        for (Schedule.Siren s : Schedule.SIRENS) {
            if (s.day() != st.day) {
                continue;
            }
            String key = s.day() + ":" + s.at();
            if (dayTick >= s.at() && dayTick < s.at() + 40 && fired.add(key)) {
                start(server, gameTime, dayTick, s.duration());
            }
        }

        if (running) {
            if (gameTime >= nextHorn && gameTime < endsAt) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.playNotifySound(ModSounds.SIREN.get(), SoundSource.MASTER, 1.0F, 1.0F);
                }
                nextHorn = gameTime + 70;
            }
            if (gameTime >= endsAt) {
                running = false;
                Msg.all(server, Component.literal("Сирена смолкла. Стало слишком тихо.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                if (st.day == 4) {
                    laughAt = gameTime + LAUGH_DELAY_TICKS;
                }
            }
        }

        if (laughAt > 0 && gameTime >= laughAt) {
            laughAt = -1;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.playNotifySound(ModSounds.CHILDREN_LAUGH.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
            }
        }

        // Тихий час: на открытом небе копится Метка, раз в секунду
        if (!running && gameTime < quietUntil && gameTime % 20 == 0) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.level().canSeeSky(p.blockPosition())) {
                    st.addMark(p.getUUID(), 1);
                }
            }
        }
    }

    public static boolean isQuiet(long gameTime) {
        return !running && gameTime < quietUntil;
    }

    private SirenManager() {}
}
