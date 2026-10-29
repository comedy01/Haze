package dev.haze.selftest;

import net.minecraft.client.gui.components.AbstractWidget;

final class Widgets {
    private Widgets() {
    }

    static int x(AbstractWidget widget) {
        return widget.getX();
    }

    static int y(AbstractWidget widget) {
        return widget.getY();
    }
}
