package ru.komchas.system;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.komchas.data.Schedule;
import ru.komchas.state.KomState;

import java.util.HashSet;
import java.util.Set;

/**
 * Расписание: в начале пары напоминает, в конце проверяет, кто пришёл.
 * Сирена посреди пары отменяет её. Прогул даёт Метку и портит доверие.
 */
public final class ScheduleManager {
    private static final Set<String> done = new HashSet<>();
    private static final Set<String> warnedZones = new HashSet<>();

    public static void onNewDay() {
        done.clear();
    }

    public static void tick(MinecraftServer server, KomState st, int dayTick) {
        for (Schedule.Event ev : Schedule.EVENTS) {
            if (ev.day() != st.day) {
                continue;
            }
            String startKey = ev.day() + ":" + ev.from() + ":start";
            String endKey = ev.day() + ":" + ev.from() + ":end";
            if (dayTick >= ev.from() && dayTick < ev.from() + 40 && done.add(startKey)) {
                Msg.all(server, Component.literal("[Расписание] " + ev.title()
                        + " с " + Msg.clock(ev.from()) + " до " + Msg.clock(ev.to()))
                        .withStyle(ChatFormatting.YELLOW));
            }
            if (dayTick >= ev.to() && dayTick < ev.to() + 40 && done.add(endKey)) {
                finish(server, st, ev);
            }
        }
    }

    private static void finish(MinecraftServer server, KomState st, Schedule.Event ev) {
        if (!ev.type().equals("class")) {
            return;
        }
        if (SirenManager.overlaps(ev.from(), ev.to())) {
            Msg.all(server, Component.literal("[Расписание] " + ev.title() + " отменена из-за сирены.")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        KomState.Zone zone = ev.zone().isEmpty() ? null : st.zones.get(ev.zone());
        if (zone == null) {
            if (!ev.zone().isEmpty() && warnedZones.add(ev.zone())) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (p.hasPermissions(2)) {
                        p.sendSystemMessage(Component.literal("[Админ] Зона «" + ev.zone()
                                + "» не задана, прогулы не считаются. Команда: /komchas zone set "
                                + ev.zone() + " 20").withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
            }
            return;
        }
        double r2 = (double) zone.r() * zone.r();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            boolean present = p.distanceToSqr(zone.x() + 0.5, zone.y(), zone.z() + 0.5) <= r2;
            if (!present) {
                st.addMark(p.getUUID(), 3);
                st.addSkip(p.getUUID());
                st.addTrust(-2);
                p.sendSystemMessage(Component.literal("Ты пропустила «" + ev.title()
                        + "». Староста запишет.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    private ScheduleManager() {}
}
