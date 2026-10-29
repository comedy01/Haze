package dev.haze.selftest;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.entity.player.ChatVisiblity;

final class Settings {
    private Settings() {
    }

    static void hideChat(Minecraft mc) {
        mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
    }

    static int fov(Minecraft mc) {
        return mc.options.fov().get();
    }

    static void fov(Minecraft mc, int fov) {
        mc.options.fov().set(fov);
    }

    static CloudStatus clouds(Minecraft mc) {
        return mc.options.cloudStatus().get();
    }

    static void clouds(Minecraft mc, CloudStatus clouds) {
        mc.options.cloudStatus().set(clouds);
    }

    static void renderDistance(Minecraft mc, int chunks) {
        mc.options.renderDistance().set(chunks);
    }

    static void command(IntegratedServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }
}
