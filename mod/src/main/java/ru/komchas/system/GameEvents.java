package ru.komchas.system;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ru.komchas.command.KomCommands;
import ru.komchas.data.Dialogues;
import ru.komchas.data.Schedule;
import ru.komchas.data.Thoughts;
import ru.komchas.state.KomState;
import ru.komchas.state.MapData;

/** Главный цикл: считает дни и вызывает менеджеры сирен, расписания, диалогов и мыслей. */
public class GameEvents {
    /** Даты дней 1..12. Правятся здесь. */
    private static final String[] DATES = {
            "Понедельник, 15 декабря", "Вторник, 16 декабря", "Среда, 17 декабря", "Четверг, 18 декабря",
            "20 декабря", "22 декабря", "24 декабря", "25 декабря",
            "26 декабря", "28 декабря", "30 декабря", "31 декабря"
    };

    @SubscribeEvent
    public void onReload(AddReloadListenerEvent event) {
        event.addListener(new Dialogues.Loader());
        event.addListener(new Thoughts.Loader());
        event.addListener(new Schedule.Loader());
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        KomCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        MinecraftServer server = level.getServer();
        KomState st = KomState.get(server);
        long gameTime = level.getGameTime();
        long dayTime = level.getDayTime();
        int dayTick = (int) (dayTime % 24000L);

        updateDay(server, st, dayTime);
        SirenManager.tick(server, st, gameTime, dayTick);
        ScheduleManager.tick(server, st, dayTick);
        DialogueManager.tick(server, st, gameTime, dayTick);
        ThoughtManager.tick(server, st, gameTime, dayTick);
        TikhiySpawner.tick(server, st, gameTime);
    }

    /** Первый вошедший становится Ветой, второй Соней. Роли хранятся по нику, поэтому ник менять нельзя. */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        KomState st = KomState.get(server);
        if (st.roles.containsKey(player.getUUID())) {
            return;
        }
        String role = st.playerWithRole("veta") == null ? "veta" : (st.playerWithRole("sonya") == null ? "sonya" : null);
        if (role == null) {
            return;
        }
        st.roles.put(player.getUUID(), role);
        st.setDirty();
        player.sendSystemMessage(Component.literal(role.equals("veta")
                ? "Ты Вета. Вторая игрок станет Соней."
                : "Ты Соня. Вета уже в квартире.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private void updateDay(MinecraftServer server, KomState st, long dayTime) {
        long idx = dayTime / 24000L;
        if (st.zones.isEmpty()) {
            MapData.applyZones(st);
        }
        if (!st.initialized) {
            st.initialized = true;
            st.lastDayIndex = idx;
            st.setDirty();
            return;
        }
        if (idx > st.lastDayIndex) {
            int advance = (int) (idx - st.lastDayIndex);
            st.lastDayIndex = idx;
            st.day = Math.min(KomState.MAX_DAY, st.day + advance);
            st.setDirty();
            SirenManager.onNewDay();
            ScheduleManager.onNewDay();
            announceDay(server, st.day);
        }
    }

    public static void announceDay(MinecraftServer server, int day) {
        String date = DATES[Math.max(0, Math.min(DATES.length - 1, day - 1))];
        Msg.title(server,
                Component.literal("День " + day).withStyle(ChatFormatting.WHITE),
                Component.literal(date).withStyle(ChatFormatting.GRAY));
    }
}
