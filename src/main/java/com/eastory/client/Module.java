package com.eastory.client;

public class Module {
    public String name;
    public boolean enabled = true;

    public Module(String name) { this.name = name; }
    public void onTick() {}
}
