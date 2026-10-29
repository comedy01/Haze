package dev.haze.neoforge;

import dev.haze.client.HazeClient;
import dev.haze.client.gui.HazeSettingsScreen;
import dev.haze.client.render.ShaderSetup;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = HazeClient.MOD_ID, dist = Dist.CLIENT)
public final class HazeNeoForge {
    public HazeNeoForge(IEventBus modBus, ModContainer container) {
        HazeClient.init(FMLPaths.CONFIGDIR.get());
        ShaderSetup.init(modBus);

        KeyMapping toggle = HazeClient.createToggleKey();
        KeyMapping settings = HazeClient.createSettingsKey();
        HazeClient.setKeys(toggle, settings);
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            event.register(toggle);
            event.register(settings);
        });

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new HazeSettingsScreen(parent, Minecraft.getInstance().options));
    }
}
