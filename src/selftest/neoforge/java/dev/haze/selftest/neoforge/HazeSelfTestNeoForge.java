package dev.haze.selftest.neoforge;

import dev.haze.selftest.HazeSelfTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "haze_selftest", dist = Dist.CLIENT)
public final class HazeSelfTestNeoForge {
    public HazeSelfTestNeoForge() {
        HazeSelfTest test = new HazeSelfTest();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> test.tick());
    }
}
