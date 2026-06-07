package com.eastory.client.bypass;

import com.eastory.client.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import java.util.Random;

public class AnticheatBypass extends ClientModule {
    private final Random random = new Random();
    private int tickCounter;
    private float yawNoise, pitchNoise;

    public AnticheatBypass() { super("AntiCheat"); }

    @Override
    public void onTick() {
        PlayerEntity player = EastoryClient.mc.player;
        if (player == null) return;
        tickCounter++;

        player.setYaw(player.getYaw() + (random.nextFloat() - 0.5f) * 0.03f);

        if (random.nextFloat() < 0.02f) {
            player.setPitch(player.getPitch() + (random.nextFloat() - 0.5f) * 0.12f);
        }

        if (tickCounter % 3 == 0) {
            player.setYaw(player.getYaw() + (random.nextFloat() - 0.5f) * 0.04f);
        }

        if (tickCounter % 4 == 0) {
            yawNoise += (random.nextFloat() - 0.5f) * 0.02f;
            yawNoise = MathHelper.clamp(yawNoise, -0.1f, 0.1f);
            pitchNoise = (random.nextFloat() - 0.5f) * 0.05f;
            player.setYaw(player.getYaw() + yawNoise);
            player.setPitch(MathHelper.clamp(player.getPitch() + pitchNoise, -90f, 90f));
        }

        if (tickCounter % 10 == 0) {
            player.setHeadYaw(player.getYaw() + (random.nextFloat() - 0.5f) * 0.01f);
        }
    }
}
