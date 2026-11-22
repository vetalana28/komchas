package ru.komchas.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.komchas.data.Dialogues;
import ru.komchas.state.KomState;
import ru.komchas.system.DialogueManager;
import ru.komchas.system.GameEvents;
import ru.komchas.system.ScheduleManager;
import ru.komchas.system.SirenManager;
import ru.komchas.system.ThoughtManager;

import java.util.List;

/** Команды /komchas. Служебные доступны операторам, /komchas choice доступна всем. */
public final class KomCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("komchas")
                // выбор в диалоге (кнопки в чате)
                .then(Commands.literal("choice")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0, 9))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    DialogueManager.choose(c.getSource().getServer(), p,
                                            IntegerArgumentType.getInteger(c, "index"));
                                    return 1;
                                })))
                // состояние
                .then(Commands.literal("state").requires(s -> s.hasPermission(2))
                        .executes(c -> {
                            MinecraftServer server = c.getSource().getServer();
                            KomState st = KomState.get(server);
                            say(c.getSource(), "День " + st.day + ", доверие " + st.trust
                                    + ", зон: " + st.zones.keySet());
                            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                say(c.getSource(), p.getName().getString()
                                        + ": роль " + st.roles.getOrDefault(p.getUUID(), "не задана")
                                        + ", Метка " + st.getMark(p.getUUID())
                                        + ", прогулов " + st.skips.getOrDefault(p.getUUID(), 0));
                            }
                            return 1;
                        }))
                // день
                .then(Commands.literal("day").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("n", IntegerArgumentType.integer(1, KomState.MAX_DAY))
                                        .executes(c -> {
                                            MinecraftServer server = c.getSource().getServer();
                                            KomState st = KomState.get(server);
                                            st.day = IntegerArgumentType.getInteger(c, "n");
                                            st.setDirty();
                                            SirenManager.onNewDay();
                                            ScheduleManager.onNewDay();
                                            GameEvents.announceDay(server, st.day);
                                            return 1;
                                        }))))
                // сирена
                .then(Commands.literal("siren").requires(s -> s.hasPermission(2))
                        .executes(c -> startSiren(c.getSource(), 40))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 300))
                                .executes(c -> startSiren(c.getSource(),
                                        IntegerArgumentType.getInteger(c, "seconds")))))
                // диалоги
                .then(Commands.literal("dialogue").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("list").executes(c -> {
                            say(c.getSource(), String.join(", ", Dialogues.ALL.keySet()));
                            return 1;
                        }))
                        .then(Commands.literal("reset").executes(c -> {
                            KomState st = KomState.get(c.getSource().getServer());
                            st.played.clear();
                            st.setDirty();
                            DialogueManager.clear();
                            say(c.getSource(), "Сыгранные диалоги сброшены.");
                            return 1;
                        }))
                        .then(Commands.literal("play")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Dialogues.ALL.keySet(), b))
                                        .executes(c -> {
                                            String id = StringArgumentType.getString(c, "id");
                                            if (!DialogueManager.enqueue(id)) {
                                                c.getSource().sendFailure(Component.literal("Нет диалога: " + id));
                                                return 0;
                                            }
                                            return 1;
                                        }))))
                // роли
                .then(Commands.literal("role").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("veta", "sonya"), b))
                                                .executes(c -> {
                                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                                    String role = StringArgumentType.getString(c, "role");
                                                    if (!role.equals("veta") && !role.equals("sonya")) {
                                                        c.getSource().sendFailure(Component.literal("Роль: veta или sonya"));
                                                        return 0;
                                                    }
                                                    KomState st = KomState.get(c.getSource().getServer());
                                                    st.roles.put(p.getUUID(), role);
                                                    st.setDirty();
                                                    say(c.getSource(), p.getName().getString() + " теперь " + role);
                                                    return 1;
                                                })))))
                // Метка и доверие
                .then(Commands.literal("mark").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("n", IntegerArgumentType.integer(-100, 100))
                                                .executes(c -> {
                                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                                    KomState st = KomState.get(c.getSource().getServer());
                                                    st.addMark(p.getUUID(), IntegerArgumentType.getInteger(c, "n"));
                                                    say(c.getSource(), "Метка " + p.getName().getString()
                                                            + ": " + st.getMark(p.getUUID()));
                                                    return 1;
                                                })))))
                .then(Commands.literal("trust").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("n", IntegerArgumentType.integer(0, 100))
                                        .executes(c -> {
                                            KomState st = KomState.get(c.getSource().getServer());
                                            st.trust = IntegerArgumentType.getInteger(c, "n");
                                            st.setDirty();
                                            say(c.getSource(), "Доверие: " + st.trust);
                                            return 1;
                                        }))))
                // зоны
                .then(Commands.literal("zone").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                List.of("flat", "yard", "college", "shop"), b))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(2, 300))
                                                .executes(c -> {
                                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                                    KomState st = KomState.get(c.getSource().getServer());
                                                    String name = StringArgumentType.getString(c, "name");
                                                    int r = IntegerArgumentType.getInteger(c, "radius");
                                                    st.zones.put(name, new KomState.Zone(
                                                            p.blockPosition().getX(), p.blockPosition().getY(),
                                                            p.blockPosition().getZ(), r));
                                                    st.setDirty();
                                                    say(c.getSource(), "Зона «" + name + "» задана здесь, радиус " + r);
                                                    return 1;
                                                })))))
                // принудительная мысль
                .then(Commands.literal("thought").requires(s -> s.hasPermission(2))
                        .executes(c -> {
                            ThoughtManager.reset();
                            say(c.getSource(), "Следующая мысль придёт в ближайшие секунды.");
                            return 1;
                        })));
    }

    private static int startSiren(CommandSourceStack src, int seconds) {
        MinecraftServer server = src.getServer();
        SirenManager.start(server, server.overworld().getGameTime(),
                (int) (server.overworld().getDayTime() % 24000L), seconds);
        return 1;
    }

    private static void say(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.GRAY), false);
    }

    private KomCommands() {}
}
