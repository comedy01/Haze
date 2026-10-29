package dev.haze.selftest;

import net.minecraft.client.gui.components.AbstractSelectionList;

final class Scrolls {
    private Scrolls() {
    }

    static double amount(AbstractSelectionList<?> list) {
        return list.getScrollAmount();
    }

    static double max(AbstractSelectionList<?> list) {
        return list.getMaxScroll();
    }
}
