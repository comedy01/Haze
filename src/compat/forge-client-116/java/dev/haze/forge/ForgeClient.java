package dev.haze.forge;

import dev.haze.client.gui.HazeSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

final class ForgeClient {
    private ForgeClient() {
    }

    static void clientOnly(ModLoadingContext context) {
        context.registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
    }

    static void keys(IEventBus modBus, KeyMapping... keys) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            for (KeyMapping key : keys) {
                ClientRegistry.registerKeyBinding(key);
            }
        }));
    }

    static void configScreen(ModLoadingContext context) {
        context.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (client, parent) -> new HazeSettingsScreen(parent, client.options));
    }
}
