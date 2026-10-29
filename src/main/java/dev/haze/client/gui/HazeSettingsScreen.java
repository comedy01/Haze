package dev.haze.client.gui;

import dev.haze.client.HazeClient;
import dev.haze.client.Texts;
import dev.haze.config.HazeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public final class HazeSettingsScreen extends HazeOptionsScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public HazeSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Texts.translatable("haze.options.title"));
    }

    public static void open(Minecraft client) {
        ScreenOpener.open(client, new HazeSettingsScreen(null, client.options));
    }

    @Override
    protected void addOptions() {
        HazeConfig config = HazeClient.config();

        addRow(
                toggle("haze.options.enabled", config::enabled, config::setEnabled),
                slider("haze.options.intensity", 10, 200, 10, config.intensity(), config::setIntensity, HazeSettingsScreen::percent));
        addRow(
                slider("haze.options.frequency", 0, 200, 10, config.frequency(), config::setFrequency,
                        HazeSettingsScreen::percentOrOff),
                slider("haze.options.depth", 8, 64, 4, config.depth(), config::setDepth, HazeSettingsScreen::blocks));

        addHeader(Texts.translatable("haze.options.section.fog"));
        addRow(
                toggle("haze.options.morning_fog", config::morningFog, config::setMorningFog),
                toggle("haze.options.rain_mist", config::rainMist, config::setRainMist));
        addRow(
                toggle("haze.options.swamp_mist", config::swampMist, config::setSwampMist),
                toggle("haze.options.sun_glow", config::sunGlow, config::setSunGlow));
        addRow(toggle("haze.options.wisps", config::wisps, config::setWisps),
                toggle("haze.options.cave_fog", config::caveFog, config::setCaveFog));
        addRow(toggle("haze.options.underwater", config::underwater, config::setUnderwater),
                toggle("haze.options.always_fog", config::alwaysFog, config::setAlwaysFog));

        addHeader(Texts.translatable("haze.options.section.other"));
        addRow(keyBindsButton(), resetButton(config));
    }

    @Override
    public void removed() {
        super.removed();
        HazeClient.saveConfig();
    }

    private AbstractWidget keyBindsButton() {
        return tooltip(button(Texts.translatable("haze.options.keys"), WIDTH,
                        button -> ScreenOpener.open(minecraft, KeyBinds.screen(this, options))),
                Texts.translatable("haze.options.keys.tooltip"));
    }

    private AbstractWidget resetButton(HazeConfig config) {
        return tooltip(button(Texts.translatable("haze.options.reset"), WIDTH, button -> {
                    config.resetToDefaults();
                    ScreenOpener.open(minecraft, new HazeSettingsScreen(lastScreen, options));
                }), Texts.translatable("haze.options.reset.tooltip"));
    }

    private static Component percent(int value) {
        return Texts.literal(value + "%");
    }

    private static Component blocks(int value) {
        return Texts.translatable("haze.options.depth.blocks", value);
    }

    private static Component percentOrOff(int value) {
        return value == 0 ? Texts.translatable("options.off") : Texts.literal(value + "%");
    }

    private AbstractWidget toggle(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        return tooltip(button(toggleLabel(key, getter.getAsBoolean()), WIDTH, button -> {
                    setter.accept(!getter.getAsBoolean());
                    button.setMessage(toggleLabel(key, getter.getAsBoolean()));
                }), Texts.translatable(key + ".tooltip"));
    }

    private static Component toggleLabel(String key, boolean on) {
        Component state = Texts.translatable(on ? "options.on" : "options.off");
        return Texts.translatable("options.generic_value", Texts.translatable(key), state);
    }

    private AbstractWidget slider(String key, int min, int max, int step, int initial, IntConsumer onChange,
                                  IntFunction<Component> format) {
        return tooltip(new ValueSlider(key, min, max, step, initial, onChange, format),
                Texts.translatable(key + ".tooltip"));
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String captionKey;
        private final int min;
        private final int max;
        private final int step;
        private final IntConsumer onChange;
        private final IntFunction<Component> format;

        ValueSlider(String captionKey, int min, int max, int step, int initial, IntConsumer onChange,
                    IntFunction<Component> format) {
            super(0, 0, WIDTH, HEIGHT, Texts.empty(), (initial - min) / (double) (max - min));
            this.captionKey = captionKey;
            this.min = min;
            this.max = max;
            this.step = step;
            this.onChange = onChange;
            this.format = format;
            updateMessage();
        }

        private int current() {
            int raw = min + (int) Math.round(value * (max - min));
            int snapped = min + Math.round((raw - min) / (float) step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        @Override
        protected void updateMessage() {
            setMessage(Texts.translatable("options.generic_value", Texts.translatable(captionKey), format.apply(current())));
        }

        @Override
        protected void applyValue() {
            int snapped = current();
            value = (snapped - min) / (double) (max - min);
            onChange.accept(snapped);
        }
    }
}
