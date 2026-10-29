package dev.haze.selftest;

import dev.haze.client.Compat;
import dev.haze.client.FogState;
import dev.haze.client.SwampMask;
import dev.haze.client.HazeClient;
import dev.haze.client.gui.HazeSettingsScreen;
import dev.haze.client.render.FogPass;
import dev.haze.config.HazeConfig;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public final class HazeSelfTest {
    private static final String WORLD = "haze-selftest";
    private static final int TIMEOUT = 2400;

    private static volatile int frames;

    private final List<Step> steps = new ArrayList<>();
    private final List<String> shots = new ArrayList<>();
    private final Set<String> existingShots = new HashSet<>();
    private final Map<String, Double> brightness = new HashMap<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int delay;
    private int waited;
    private int seenFrames;
    private int patience;
    private int fov;
    private CloudStatus clouds;
    private int passFrames;
    private static int ground;

    private record Step(int delay, BooleanSupplier ready, Runnable action) {
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 40) {
                started = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                Settings.hideChat(mc);
                fov = Settings.fov(mc);
                clouds = Settings.clouds(mc);
                rememberShots(mc);
                plan(mc);
                delay = steps.get(0).delay();
                log("creating world");
                deleteWorld(mc, WORLD);
                boolean scenic = System.getProperty("haze.only", "").equals("voxy");
                Worlds.create(mc, WORLD, !scenic, scenic ? Long.getLong("haze.seed", 3L) : 0L);
            }
            return;
        }
        int drawn = frames;
        if (drawn == seenFrames) {
            return;
        }
        seenFrames = drawn;
        if (delay > 0) {
            delay--;
            return;
        }
        Step step = steps.get(index);
        if (!step.ready().getAsBoolean()) {
            if (++waited > TIMEOUT) {
                finish(mc, new AssertionError("timed out at step " + index));
            }
            return;
        }
        waited = 0;
        index++;
        try {
            step.action().run();
        } catch (Throwable e) {
            finish(mc, e);
            return;
        }
        if (index >= steps.size()) {
            finish(mc, null);
        } else {
            delay = steps.get(index).delay();
        }
    }

    public static void frame() {
        frames++;
    }

    private void then(int ticks, Runnable action) {
        steps.add(new Step(ticks, () -> true, action));
    }

    private void when(BooleanSupplier ready, Runnable action) {
        steps.add(new Step(0, ready, action));
    }

    private void within(int ticks, BooleanSupplier ready, Runnable action) {
        then(1, () -> patience = ticks);
        when(() -> ready.getAsBoolean() || --patience < 0, action);
    }

    private void finish(Minecraft mc, Throwable failure) {
        finished = true;
        nameShots(mc);
        log("fog draws: " + FogPass.frames() + (FogPass.broken() ? " (BROKEN)" : ""));
        if (failure == null) {
            log("ALL CHECKS PASSED");
        } else {
            log("FAILED: " + failure);
            failure.printStackTrace();
        }
        HazeClient.config().resetToDefaults();
        HazeClient.saveConfig();
        Settings.fov(mc, fov);
        Settings.clouds(mc, clouds);
        mc.stop();
    }

    private void plan(Minecraft mc) {
        HazeConfig config = HazeClient.config();
        when(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null,
                () -> log("world loaded; shader pack " + Compat.shaderPackInUse()));
        then(40, () -> {
            config.resetToDefaults();
            ground = (int) Math.floor(mc.player.getY());
            run(mc, "gamerule advance_time false");
            run(mc, "gamerule advance_weather false");
            run(mc, "gamerule spawn_mobs false");
            run(mc, "gamerule doDaylightCycle false");
            run(mc, "gamerule doWeatherCycle false");
            run(mc, "gamerule doMobSpawning false");
            run(mc, "weather clear 1000000");
            run(mc, "time set 6000");
            run(mc, "gamemode spectator @p");
            run(mc, "tp @p 0.5 " + (ground + 5) + " 0.5 180 10");
            Settings.clouds(mc, CloudStatus.OFF);
            Screens.hideHud(mc);
        });
        String only = System.getProperty("haze.only", "");
        if (only.equals("voxy")) {
            planVoxy(mc, config);
            return;
        }
        if (only.equals("walk")) {
            planWalk(mc, config);
            return;
        }
        if (only.equals("perf")) {
            planPerf(mc, config);
            return;
        }
        if (!only.equals("settings")) {
            planFog(mc, config);
        }
        if (!only.equals("fog")) {
            then(5, config::resetToDefaults);
            planSettings(mc, config);
        }
    }

    private void planFog(Minecraft mc, HazeConfig config) {
        within(600, () -> FogState.density < 0.002F && !FogState.view(0F).visible(), () -> {
            check(FogState.density < 0.002F, "a clear plains noon had fog: " + FogState.density);
            passFrames = FogPass.frames();
            shoot(mc, "clear-noon");
        });
        then(20, () -> {
            check(FogPass.frames() == passFrames, "the fog pass drew with no fog");
            BiomeFill.fill(mc, "minecraft:swamp");
            run(mc, "time set 18000");
        });
        within(1200, () -> FogState.swamp > 0.15F, () -> {
            check(FogState.swamp > 0.15F, "a swamp night was not misty: " + FogState.swamp);
            check(SwampMask.count > 0, "the swamp mask found no swamp");
            passFrames = FogPass.frames();
        });
        then(30, () -> {
            check(!FogPass.broken(), "the fog pass broke");
            check(FogPass.frames() > passFrames, "the fog pass did not draw in a misty swamp");
            shoot(mc, "swamp-night");
            config.setEnabled(false);
        });
        then(20, () -> {
            check(FogState.density == 0F, "fog stayed with Haze turned off: " + FogState.density);
            passFrames = FogPass.frames();
        });
        then(20, () -> {
            check(FogPass.frames() == passFrames, "the fog pass drew with Haze turned off");
            config.setEnabled(true);
            config.setFrequency(200);
            config.setSwampMist(false);
            BiomeFill.fill(mc, "minecraft:plains");
            Screens.turn(mc, -90.0F, 5.0F);
        });
        within(200, () -> FogState.swamp < 0.002F, () -> check(FogState.swamp < 0.002F, "swamp mist lingered: " + FogState.swamp));
        BooleanSupplier foggyDawn = () -> FogState.warmth > 0.05F && FogState.density > 0.2F;
        then(1, () -> run(mc, "time set 23500"));
        for (int day = 1; day < 8; day++) {
            long time = 23500L + day * 24000L;
            within(900, foggyDawn, () -> {
                if (!foggyDawn.getAsBoolean()) {
                    run(mc, "time set " + time);
                }
            });
        }
        within(900, foggyDawn, () -> {
            check(FogState.warmth > 0.05F, "no warm sunrise glow on a foggy morning: " + FogState.warmth);
            check(FogState.density > 0.2F, "no thick morning fog after 8 dawns: " + FogState.density);
            shoot(mc, "dawn-fog");
            config.setFrequency(0);
            run(mc, "time set 6000");
            lookUp(mc);
        });
        within(1200, () -> FogState.density < 0.002F, () -> {
            check(FogState.density < 0.002F, "fog did not clear at noon: " + FogState.density);
            run(mc, "weather rain 1000000");
        });
        within(1500, () -> FogState.density > 0.1F, () -> {
            check(FogState.density > 0.1F, "rain brought no mist: " + FogState.density);
            shoot(mc, "rain-mist");
            BiomeFill.fill(mc, "minecraft:desert");
        });
        within(1200, () -> FogState.density < 0.002F, () -> {
            check(FogState.density < 0.002F, "a desert had mist: " + FogState.density);
            run(mc, "weather clear 1000000");
            config.resetToDefaults();
            log("fog checks done");
        });
    }

    private void planVoxy(Minecraft mc, HazeConfig config) {
        boolean[] voxy = new boolean[1];
        then(20, () -> {
            voxy[0] = lodFix();
            Settings.renderDistance(mc, 24);
            run(mc, "tp @p 0.5 " + (ground + 30) + " 0.5 180 8");
        });
        then(2400, () -> Settings.renderDistance(mc, 8));
        then(600, () -> shoot(mc, (voxy[0] ? "voxy-" : "vanilla-") + "early"));
        then(1800, () -> shoot(mc, (voxy[0] ? "voxy-" : "vanilla-") + "clear"));
        then(1, () -> config.setAlwaysFog(true));
        within(900, () -> FogState.density > 0.3F, () -> log("always fog density " + FogState.density));
        then(200, () -> lodFix(false));
        then(60, () -> shoot(mc, (voxy[0] ? "voxy-" : "vanilla-") + "fog-old"));
        then(1, () -> lodFix(voxy[0]));
        then(60, () -> shoot(mc, (voxy[0] ? "voxy-" : "vanilla-") + "fog-new"));
        then(20, () -> Settings.renderDistance(mc, 12));
    }

    private static boolean lodFix() {
        try {
            java.lang.reflect.Field field = dev.haze.client.HazeRuntime.class.getDeclaredField("voxy");
            field.setAccessible(true);
            return field.getBoolean(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void lodFix(boolean on) {
        try {
            java.lang.reflect.Field field = dev.haze.client.HazeRuntime.class.getDeclaredField("voxy");
            field.setAccessible(true);
            field.setBoolean(null, on);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void planPerf(Minecraft mc, HazeConfig config) {
        then(20, () -> {
            BiomeFill.fill(mc, "minecraft:swamp");
            run(mc, "time set 18000");
        });
        within(1200, () -> FogState.density > 0.15F, () -> log("perf density " + FogState.density));
        then(400, () -> shoot(mc, "perf-fog"));
        measure("fog on");
        then(1, () -> config.setEnabled(false));
        then(60, () -> shoot(mc, "perf-off"));
        measure("fog off");
        then(1, () -> config.setEnabled(true));
        then(120, () -> {
        });
        measure("fog on again");
    }

    private void measure(String name) {
        long[] start = new long[2];
        then(60, () -> {
            start[0] = System.nanoTime();
            start[1] = frames;
            FogTimer.reset();
        });
        then(600, () -> {
            double ms = (System.nanoTime() - start[0]) / 1e6 / (frames - start[1]);
            log(String.format(java.util.Locale.ROOT, "perf %s: %.3f ms/frame (%.0f fps), fog pass gpu %.3f ms over %d samples",
                    name, ms, 1000 / ms, FogTimer.averageMs(), FogTimer.count()));
        });
    }

    private void planWalk(Minecraft mc, HazeConfig config) {
        then(20, () -> {
            BiomeFill.fill(mc, "minecraft:forest");
            plant(mc, "minecraft:oak", "minecraft:birch", "minecraft:fancy_oak");
            run(mc, "gamemode creative @p");
            run(mc, "tp @p 0.5 " + ground + " 0.5 -90 2");
            config.setFrequency(200);
        });
        for (int day = 0; day < 8; day++) {
            long time = 23500L + day * 24000L;
            then(day == 0 ? 60 : 900, () -> {
                if (FogState.warmth < 0.05F || FogState.density < 0.3F) {
                    run(mc, "time set " + time);
                }
            });
        }
        then(900, () -> {
            log("walk density " + FogState.density);
            mc.options.keyUp.setDown(true);
        });
        for (int i = 0; i < 60; i++) {
            int n = i;
            then(1, () -> shoot(mc, String.format(Locale.ROOT, "walk-%02d", n)));
        }
        then(1, () -> mc.options.keyUp.setDown(false));
        then(40, () -> log("walk done"));
    }

    private static void plant(Minecraft mc, String... trees) {
        java.util.Random random = new java.util.Random(7L);
        for (int x = 6; x < 96; x += 7) {
            for (int z = -48; z < 48; z += 7) {
                int dx = x + random.nextInt(4);
                int dz = z + random.nextInt(4);
                String tree = trees[random.nextInt(trees.length)];
                run(mc, "place feature " + tree + " " + dx + " " + ground + " " + dz);
            }
        }
    }

    private static void lookUp(Minecraft mc) {
        Screens.turn(mc, 180.0F, 10.0F);
    }

    private void planSettings(Minecraft mc, HazeConfig config) {
        then(20, () -> Screens.open(mc, new HazeSettingsScreen(null, mc.options)));
        then(10, () -> {
            shoot(mc, "settings");
            clickRightEdge(mc, slider(Screens.current(mc), "Thickness"));
        });
        then(3, () -> {
            check(config.intensity() == 200, "the Thickness slider did not reach 200: " + config.intensity());
            click(mc, find(Screens.current(mc), "haze.options.wisps"));
        });
        then(3, () -> {
            check(!config.wisps(), "the Wisps button did not switch them off");
            click(mc, find(Screens.current(mc), "haze.options.morning_fog"));
        });
        then(3, () -> {
            check(!config.morningFog(), "the Morning Fog button did not switch it off");
            scroll(Screens.current(mc), true);
        });
        then(3, () -> click(mc, find(Screens.current(mc), "haze.options.reset")));
        then(5, () -> {
            check(config.wisps(), "reset did not switch Wisps back on");
            check(config.morningFog(), "reset did not switch Morning Fog back on");
            check(config.intensity() == 100, "reset did not restore Thickness");
            scroll(Screens.current(mc), false);
        });
        then(3, () -> clickRightEdge(mc, slider(Screens.current(mc), "Foggy Mornings")));
        then(3, () -> {
            check(config.frequency() == 200, "the Foggy Mornings slider did not reach 200: " + config.frequency());
            Screens.open(mc, null);
        });
        then(5, () -> {
            String saved = read(HazeClient.configPath());
            check(saved.contains("\"frequency\": 200"), "settings were not saved on close: " + saved);
        });
        then(10, () -> log("settings checks done"));
    }

    private void revealing(Minecraft mc, String key) {
        for (int i = 0; i < 8; i++) {
            then(2, () -> reveal(Screens.current(mc), find(Screens.current(mc), key)));
        }
    }

    private static void reveal(Screen screen, AbstractWidget widget) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractSelectionList<?> list && !Lists.shows(list, widget)) {
                Lists.reveal(list, widget);
            }
        }
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> Settings.command(server, command));
    }

    private void shoot(Minecraft mc, String name) {
        log("screenshot: " + name);
        shots.add(name);
        File dir = new File(mc.gameDirectory, "screenshots");
        Screens.shot(mc);
        pending.put(name, dir);
    }

    private final Map<String, File> pending = new HashMap<>();

    private double average(String name) {
        Double known = brightness.get(name);
        if (known != null) {
            return known;
        }
        File file = newestFor(name);
        check(file != null, "no screenshot file for " + name);
        try {
            BufferedImage image = ImageIO.read(file);
            long sum = 0;
            long n = 0;
            int h = image.getHeight() * 3 / 4;
            for (int y = 0; y < h; y += 4) {
                for (int x = 0; x < image.getWidth(); x += 4) {
                    int rgb = image.getRGB(x, y);
                    sum += ((rgb >> 16) & 255) + ((rgb >> 8) & 255) + (rgb & 255);
                    n += 3;
                }
            }
            double value = sum / (double) n;
            brightness.put(name, value);
            log(String.format(Locale.ROOT, "brightness %s: %.2f", name, value));
            return value;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BufferedImage image(String name) {
        File file = newestFor(name);
        check(file != null, "no screenshot file for " + name);
        BufferedImage image = null;
        IOException failure = null;
        for (int attempt = 0; attempt < 40 && image == null; attempt++) {
            try {
                image = ImageIO.read(file);
            } catch (IOException e) {
                failure = e;
            }
            if (image == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (image == null && failure != null) {
            throw new UncheckedIOException(failure);
        }
        check(image != null, "the screenshot for " + name + " could not be read: " + file.getName());
        return image;
    }

    private int pixels(String name, java.util.function.IntPredicate test) {
        BufferedImage image = image(name);
        int count = 0;
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                if (test.test(image.getRGB(x, y))) {
                    count++;
                }
            }
        }
        log("pixels " + name + ": " + count);
        return count;
    }

    private double centre(String name) {
        BufferedImage img = image(name);
        long sum = 0;
        long n = 0;
        for (int y = img.getHeight() * 3 / 10; y < img.getHeight() * 7 / 10; y += 3) {
            for (int x = img.getWidth() * 3 / 10; x < img.getWidth() * 7 / 10; x += 3) {
                int rgb = img.getRGB(x, y);
                sum += red(rgb) * 3 + green(rgb) * 6 + blue(rgb);
                n += 10;
            }
        }
        return sum / (double) n;
    }

    private double difference(String a, String b) {
        BufferedImage first = image(a);
        BufferedImage second = image(b);
        long sum = 0;
        long n = 0;
        for (int y = 0; y < Math.min(first.getHeight(), second.getHeight()); y += 4) {
            for (int x = 0; x < Math.min(first.getWidth(), second.getWidth()); x += 4) {
                int p = first.getRGB(x, y);
                int q = second.getRGB(x, y);
                for (int shift = 0; shift <= 16; shift += 8) {
                    sum += Math.abs(((p >> shift) & 255) - ((q >> shift) & 255));
                    n++;
                }
            }
        }
        double value = sum / (double) n;
        log(String.format(Locale.ROOT, "difference %s vs %s: %.2f", a, b, value));
        return value;
    }

    private static int red(int rgb) {
        return (rgb >> 16) & 255;
    }

    private static int green(int rgb) {
        return (rgb >> 8) & 255;
    }

    private static int blue(int rgb) {
        return rgb & 255;
    }

    private File newestFor(String name) {
        int position = shots.lastIndexOf(name);
        File dir = new File(Minecraft.getInstance().gameDirectory, "screenshots");
        File[] files = dir.listFiles((parent, file) -> file.endsWith(".png") && !existingShots.contains(file)
                && !file.startsWith("hz-"));
        if (files == null || files.length <= position) {
            return null;
        }
        List<File> fresh = new ArrayList<>(List.of(files));
        fresh.sort(Comparator.comparing(File::getName));
        return fresh.get(position);
    }

    private void rememberShots(Minecraft mc) {
        File[] files = new File(mc.gameDirectory, "screenshots").listFiles();
        if (files != null) {
            for (File file : files) {
                existingShots.add(file.getName());
            }
        }
    }

    private void nameShots(Minecraft mc) {
        File dir = new File(mc.gameDirectory, "screenshots");
        File[] files = dir.listFiles((parent, name) -> name.endsWith(".png") && !existingShots.contains(name)
                && !name.startsWith("hz-"));
        if (files == null) {
            return;
        }
        List<File> fresh = new ArrayList<>(List.of(files));
        fresh.sort(Comparator.comparing(File::getName));
        for (int i = 0; i < fresh.size() && i < shots.size(); i++) {
            File target = new File(dir, String.format(Locale.ROOT, "hz-%02d-%s.png", i, shots.get(i)));
            if ((!target.exists() || target.delete()) && fresh.get(i).renameTo(target)) {
                log("saved " + target.getName());
            }
        }
    }

    private static void deleteWorld(Minecraft mc, String name) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(name);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void click(Minecraft mc, AbstractWidget widget) {
        visible(Screens.current(mc), widget);
        Ui.click(Screens.current(mc), Widgets.x(widget) + widget.getWidth() / 2.0, Widgets.y(widget) + widget.getHeight() / 2.0);
    }

    private static void clickRightEdge(Minecraft mc, AbstractWidget widget) {
        visible(Screens.current(mc), widget);
        Ui.click(Screens.current(mc), Widgets.x(widget) + widget.getWidth() - 2.0, Widgets.y(widget) + widget.getHeight() / 2.0);
    }

    private static void scroll(Screen screen, boolean bottom) {
        boolean listed = false;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractSelectionList<?> list) {
                list.setScrollAmount(bottom ? Double.MAX_VALUE : 0.0);
                listed = true;
            }
        }
        if (!listed) {
            try {
                screen.getClass().getMethod("scrollTo", boolean.class).invoke(screen, bottom);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void visible(Screen screen, AbstractWidget widget) {
        check(shown(widget), widget.getMessage().getString() + " is hidden");
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractSelectionList<?> list) {
                check(Lists.shows(list, widget), widget.getMessage().getString() + " is scrolled out of view");
            }
        }
    }

    private static boolean shown(AbstractWidget widget) {
        try {
            return (boolean) AbstractWidget.class.getMethod("isVisible").invoke(widget);
        } catch (ReflectiveOperationException e) {
            try {
                return AbstractWidget.class.getDeclaredField("visible").getBoolean(widget);
            } catch (ReflectiveOperationException f) {
                throw new IllegalStateException(f);
            }
        }
    }

    static AbstractWidget find(GuiEventListener node, String key) {
        if (node instanceof AbstractWidget widget && labelled(widget, key)) {
            return widget;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractWidget found = find(child, key);
                if (found != null) {
                    return found;
                }
            }
        }
        if (node instanceof Screen) {
            throw new AssertionError("no widget " + key + " on " + node);
        }
        return null;
    }

    private static boolean labelled(AbstractWidget widget, String key) {
        if (key.equals(TextKeys.key(widget.getMessage()))) {
            return true;
        }
        Object[] args = TextKeys.args(widget.getMessage());
        return args.length > 0
                && args[0] instanceof Component name
                && key.equals(TextKeys.key(name));
    }

    private static AbstractSliderButton slider(GuiEventListener node, String caption) {
        AbstractSliderButton found = findSlider(node, caption);
        check(found != null, "no slider " + caption);
        return found;
    }

    private static AbstractSliderButton findSlider(GuiEventListener node, String caption) {
        if (node instanceof AbstractSliderButton slider && slider.getMessage().getString().startsWith(caption + ":")) {
            return slider;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractSliderButton found = findSlider(child, caption);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void log(String message) {
        System.out.println("[haze-selftest] " + message);
    }
}
