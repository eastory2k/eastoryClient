package com.eastory.client;

import net.minecraft.client.option.KeyBinding;

public class ClientModule {
    public String name;
    public boolean enabled = true;
    public KeyBinding keyBinding;

    public ClientModule(String name) { this.name = name; }
    public void onTick() {}
}
