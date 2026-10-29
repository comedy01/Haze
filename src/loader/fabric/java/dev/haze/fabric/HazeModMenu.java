package dev.haze.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.haze.client.gui.HazeSettingsScreen;
import net.minecraft.client.Minecraft;

public final class HazeModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new HazeSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
