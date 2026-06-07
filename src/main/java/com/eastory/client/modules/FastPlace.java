package com.eastory.client.modules;

import com.eastory.client.*;
import com.eastory.client.mixin.MinecraftClientMixin;

public class FastPlace extends Module {
    public FastPlace() { super("FastPlace"); }

    @Override
    public void onTick() {
        if (EastoryClient.mc.player == null) return;
        if (EastoryClient.mc.options.useKey.isPressed()) {
            ((MinecraftClientMixin) EastoryClient.mc).setCooldown(0);
        }
    }
}
