package dev.haze.selftest;

import java.lang.reflect.Field;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;

final class Lists {
    private Lists() {
    }

    static boolean shows(AbstractSelectionList<?> list, AbstractWidget widget) {
        return widget.y >= top(list) && widget.y + widget.getHeight() <= bottom(list);
    }

    static void reveal(AbstractSelectionList<?> list, AbstractWidget widget) {
        int height = bottom(list) - top(list);
        if (widget.y <= 0) {
            list.setScrollAmount(Scrolls.amount(list) + height * 0.5);
            return;
        }
        double target = Scrolls.amount(list) + widget.y - top(list) - height / 2.0;
        list.setScrollAmount(Math.max(0.0, target));
    }

    private static int top(AbstractSelectionList<?> list) {
        return field(list, "y0");
    }

    private static int bottom(AbstractSelectionList<?> list) {
        return field(list, "y1");
    }

    private static int field(AbstractSelectionList<?> list, String name) {
        try {
            Field f = AbstractSelectionList.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(list);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
