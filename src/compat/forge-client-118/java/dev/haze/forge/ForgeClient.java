package dev.haze.forge;

import dev.haze.client.gui.HazeSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            for (KeyMapping key : keys) {
                ClientRegistry.registerKeyBinding(key);
            }
        }));
    }

    static void configScreen(ModLoadingContext context) {
        context.registerExtensionPoint(
                ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory(
                        (client, parent) -> new HazeSettingsScreen(parent, client.options)));
    }
}
