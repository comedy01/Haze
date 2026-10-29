package dev.haze.selftest.fabric;

import dev.haze.selftest.HazeSelfTest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class HazeSelfTestFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HazeSelfTest test = new HazeSelfTest();
        ClientTickEvents.END_CLIENT_TICK.register(client -> test.tick());
    }
}
