package dev.haze.selftest;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class Ui {
    private Ui() {
    }

    static void click(Screen screen, double x, double y) {
        MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }
}
