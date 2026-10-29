package dev.haze.selftest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;

final class Screens {
    private Screens() {
    }

    static Screen current(Minecraft mc) {
        return mc.gui.screen();
    }

    static void open(Minecraft mc, Screen screen) {
        mc.gui.setScreen(screen);
    }

    static Overlay overlay(Minecraft mc) {
        return mc.gui.overlay();
    }

    static void hideHud(Minecraft mc) {
        if (!mc.gui.hud.isHidden()) {
            mc.gui.hud.toggle();
        }
    }

    static RenderTarget renderTarget(Minecraft mc) {
        return mc.gameRenderer.mainRenderTarget();
    }

    static void turn(Minecraft mc, float yaw, float pitch) {
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    static void shot(Minecraft mc) {
        Screenshot.grab(mc.gameDirectory, renderTarget(mc), message -> {
        });
    }
}
