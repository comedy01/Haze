package dev.haze.forge;

import dev.haze.client.HazeClient;
import dev.haze.client.render.ShaderSetup;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(HazeClient.MOD_ID)
public final class HazeForge {
    public HazeForge() {
        ModLoadingContext context = ModLoadingContext.get();
        ForgeClient.clientOnly(context);
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        HazeClient.init(FMLPaths.CONFIGDIR.get());
        ShaderSetup.init(modBus);

        KeyMapping toggle = HazeClient.createToggleKey();
        KeyMapping settings = HazeClient.createSettingsKey();
        HazeClient.setKeys(toggle, settings);
        ForgeClient.keys(modBus, toggle, settings);
        ForgeClient.configScreen(context);
    }
}
