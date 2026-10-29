package dev.haze.forge;

import dev.haze.client.gui.HazeSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.NetworkConstants;

final class ForgeClient {
    private ForgeClient() {
    }

    static void clientOnly(ModLoadingContext context) {
        context.registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
    }

    static void keys(IEventBus modBus, KeyMapping... keys) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            for (KeyMapping key : keys) {
                event.register(key);
            }
        });
    }

    static void configScreen(ModLoadingContext context) {
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> new HazeSettingsScreen(parent, client.options)));
    }
}
