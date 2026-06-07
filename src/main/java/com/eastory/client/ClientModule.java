package com.eastory.client;

public class ClientModule {
    public String name;
    public boolean enabled = true;

    public ClientModule(String name) { this.name = name; }
    public void onTick() {}
}
