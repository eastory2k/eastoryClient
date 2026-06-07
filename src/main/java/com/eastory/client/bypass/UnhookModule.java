package com.eastory.client.bypass;

import com.eastory.client.*;
import net.minecraft.client.MinecraftClient;

public class UnhookModule extends ClientModule {
    private boolean done;
    public UnhookModule() { super("Unhook"); }

    @Override
    public void onTick() {
        if (done) return;
        done = true;
        try {
            long h = MinecraftClient.getInstance().getWindow().getHandle();
            com.sun.jna.NativeLibrary.getInstance("user32")
                .getFunction("SetWindowDisplayAffinity")
                .invoke(int.class, new Object[]{h, 0x00000011});
        } catch (Exception ignored) {}
    }
}
