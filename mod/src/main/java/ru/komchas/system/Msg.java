package ru.komchas.system;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Мелкие помощники для рассылки сообщений. */
public final class Msg {
    public static void all(MinecraftServer server, Component text) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(text);
        }
    }

    public static void actionBar(ServerPlayer player, Component text) {
        player.displayClientMessage(text, true);
    }

    public static void title(MinecraftServer server, Component title, Component subtitle) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    /** Тики суток (0 = 6:00) в формат ЧЧ:ММ. */
    public static String clock(int dayTick) {
        int hour = (dayTick / 1000 + 6) % 24;
        int minute = (dayTick % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hour, minute);
    }

    private Msg() {}
}
