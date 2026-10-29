package dev.haze.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

final class Messages {
    private Messages() {
    }

    static void overlay(LocalPlayer player, Component message) {
        player.sendOverlayMessage(message);
    }
}
