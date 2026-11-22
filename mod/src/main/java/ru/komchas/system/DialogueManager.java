package ru.komchas.system;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;
import ru.komchas.data.Dialogues;
import ru.komchas.state.KomState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Проигрыватель диалогов. Одновременно идёт один диалог, остальные ждут в очереди.
 * Реплики идут в чат, мысли над хотбаром, выбор в виде кликабельных кнопок.
 */
public final class DialogueManager {
    private static Dialogues.Dialogue current;
    private static int index;
    private static long nextTick;
    private static boolean waitingChoice;
    private static final Deque<String> QUEUE = new ArrayDeque<>();

    public static boolean enqueue(String id) {
        if (!Dialogues.ALL.containsKey(id)) {
            return false;
        }
        QUEUE.add(id);
        return true;
    }

    public static void clear() {
        current = null;
        waitingChoice = false;
        QUEUE.clear();
    }

    public static void tick(MinecraftServer server, KomState st, long gameTime, int dayTick) {
        if (current == null) {
            if (!QUEUE.isEmpty()) {
                begin(QUEUE.poll(), gameTime);
            } else if (gameTime % 20 == 0) {
                checkTriggers(server, st, gameTime, dayTick);
            }
            return;
        }
        if (waitingChoice || gameTime < nextTick) {
            return;
        }
        List<Dialogues.Line> lines = current.lines();
        if (index >= lines.size()) {
            if (!current.choices().isEmpty()) {
                showChoices(server);
                waitingChoice = true;
            } else {
                current = null;
            }
            return;
        }
        deliver(server, st, lines.get(index++));
        nextTick = index < lines.size() ? gameTime + lines.get(index).delay() : gameTime + 10;
    }

    private static void begin(String id, long gameTime) {
        Dialogues.Dialogue d = Dialogues.ALL.get(id);
        if (d == null) {
            return;
        }
        current = d;
        index = 0;
        waitingChoice = false;
        nextTick = gameTime + (d.lines().isEmpty() ? 0 : d.lines().get(0).delay());
    }

    private static void checkTriggers(MinecraftServer server, KomState st, long gameTime, int dayTick) {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        for (Dialogues.Dialogue d : Dialogues.ALL.values()) {
            Dialogues.Trigger t = d.trigger();
            if (t == null) {
                continue;
            }
            String key = d.id() + "@" + (d.once() ? 0 : st.day);
            if (st.played.contains(key)) {
                continue;
            }
            if (t.day() != 0 && t.day() != st.day) {
                continue;
            }
            if (dayTick < t.from() || dayTick > t.to()) {
                continue;
            }
            if (!zoneOk(server, st, t.zone())) {
                continue;
            }
            st.played.add(key);
            st.setDirty();
            begin(d.id(), gameTime);
            return;
        }
    }

    /** Если зона не задана командой, привязка к месту игнорируется. */
    private static boolean zoneOk(MinecraftServer server, KomState st, String name) {
        if (name == null || name.isEmpty()) {
            return true;
        }
        KomState.Zone z = st.zones.get(name);
        if (z == null) {
            return true;
        }
        double r2 = (double) z.r() * z.r();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.distanceToSqr(z.x() + 0.5, z.y(), z.z() + 0.5) > r2) {
                return false;
            }
        }
        return true;
    }

    private static void deliver(MinecraftServer server, KomState st, Dialogues.Line line) {
        switch (line.kind()) {
            case "think" -> thought(server, st, line);
            case "narrate" -> Msg.all(server,
                    Component.literal(line.text()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            case "sound" -> playSound(server, line.text());
            case "siren" -> {
                long gt = server.overworld().getGameTime();
                int dayTick = (int) (server.overworld().getDayTime() % 24000L);
                SirenManager.start(server, gt, dayTick, parseSeconds(line.text()));
            }
            default -> Msg.all(server, speech(line));
        }
    }

    private static int parseSeconds(String s) {
        try {
            return Math.max(5, Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return 40;
        }
    }

    private static void thought(MinecraftServer server, KomState st, Dialogues.Line line) {
        Component text = Component.literal(line.text()).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
        UUID target = st.playerWithRole(line.speaker());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (target == null || target.equals(p.getUUID())) {
                Msg.actionBar(p, text);
            }
        }
    }

    private static void playSound(MinecraftServer server, String id) {
        SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(id));
        if (ev == null) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.playNotifySound(ev, SoundSource.MASTER, 1.0F, 1.0F);
        }
    }

    private static Component speech(Dialogues.Line line) {
        String name;
        ChatFormatting color;
        switch (line.speaker()) {
            case "veta" -> { name = "Вета"; color = ChatFormatting.LIGHT_PURPLE; }
            case "sonya" -> { name = "Соня"; color = ChatFormatting.AQUA; }
            default -> { name = line.speaker().isEmpty() ? "???" : line.speaker(); color = ChatFormatting.GOLD; }
        }
        return Component.literal(name + ": ").withStyle(color)
                .append(Component.literal(line.text()).withStyle(ChatFormatting.WHITE));
    }

    private static void showChoices(MinecraftServer server) {
        MutableComponent row = Component.literal("");
        List<Dialogues.Option> opts = current.choices();
        for (int i = 0; i < opts.size(); i++) {
            Style style = Style.EMPTY
                    .withColor(ChatFormatting.YELLOW)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/komchas choice " + i));
            row.append(Component.literal("[" + opts.get(i).label() + "]").withStyle(style));
            row.append(Component.literal("  "));
        }
        Msg.all(server, row);
    }

    public static void choose(MinecraftServer server, ServerPlayer player, int optionIndex) {
        if (current == null || !waitingChoice) {
            return;
        }
        List<Dialogues.Option> opts = current.choices();
        if (optionIndex < 0 || optionIndex >= opts.size()) {
            return;
        }
        Dialogues.Option o = opts.get(optionIndex);
        KomState st = KomState.get(server);
        if (o.mark() != 0) {
            st.addMark(player.getUUID(), o.mark());
        }
        if (o.trust() != 0) {
            st.addTrust(o.trust());
        }
        current = null;
        waitingChoice = false;
        if (!o.next().isEmpty()) {
            enqueue(o.next());
        }
    }

    private DialogueManager() {}
}
