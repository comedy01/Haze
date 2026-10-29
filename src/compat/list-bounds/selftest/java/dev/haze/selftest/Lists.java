package dev.haze.selftest;

import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;

final class Lists {
    private Lists() {
    }

    static boolean shows(AbstractSelectionList<?> list, AbstractWidget widget) {
        return widget.getY() >= list.getY() && widget.getY() + widget.getHeight() <= list.getY() + list.getHeight();
    }

    static void reveal(AbstractSelectionList<?> list, AbstractWidget widget) {
        if (widget.getY() <= 0) {
            list.setScrollAmount(Scrolls.amount(list) + list.getHeight() * 0.5);
            return;
        }
        double target = Scrolls.amount(list) + widget.getY() - list.getY() - list.getHeight() / 2.0;
        list.setScrollAmount(Math.max(0.0, target));
    }
}
