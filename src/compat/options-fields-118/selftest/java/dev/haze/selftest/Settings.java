package dev.haze.selftest;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.entity.player.ChatVisiblity;

final class Settings {
    private Settings() {
    }

    static void hideChat(Minecraft mc) {
        mc.options.chatVisibility = ChatVisiblity.HIDDEN;
    }

    static int fov(Minecraft mc) {
        return (int) mc.options.fov;
    }

    static void fov(Minecraft mc, int fov) {
        mc.options.fov = fov;
    }

    static CloudStatus clouds(Minecraft mc) {
        return mc.options.renderClouds;
    }

    static void clouds(Minecraft mc, CloudStatus clouds) {
        mc.options.renderClouds = clouds;
    }

    static void renderDistance(Minecraft mc, int chunks) {
        mc.options.renderDistance = chunks;
    }

    static void command(IntegratedServer server, String command) {
        server.getCommands().performCommand(server.createCommandSourceStack(), command);
    }
}
