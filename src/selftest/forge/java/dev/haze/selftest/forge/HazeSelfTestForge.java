package dev.haze.selftest.forge;

import dev.haze.selftest.HazeSelfTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("haze_selftest")
public final class HazeSelfTestForge {
    public HazeSelfTestForge() {
        HazeSelfTest test = new HazeSelfTest();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                test.tick();
            }
        });
    }
}
