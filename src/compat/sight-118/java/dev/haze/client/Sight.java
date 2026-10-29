package dev.haze.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class Sight {
    private Sight() {
    }

    public static boolean blind(Player player) {
        return player.hasEffect(MobEffects.BLINDNESS);
    }

    public static Vec3 eye(Player player) {
        return player.getEyePosition();
    }

    public static boolean stuck(BlockState state) {
        return state.is(Blocks.POWDER_SNOW);
    }

    public static float viewDistance(Minecraft mc) {
        return mc.options.getEffectiveRenderDistance() * 16F;
    }
}
