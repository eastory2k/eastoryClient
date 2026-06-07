package com.eastory.client;

import java.util.*;

public class Config {
    public static Map<String, Boolean> toggles = new HashMap<>();
    public static Map<String, Float> values = new HashMap<>();

    static {
        toggles.put("KillAura", true);
        toggles.put("TriggerBot", true);
        toggles.put("AimAssist", true);
        toggles.put("AutoSword", true);
        toggles.put("FastPlace", true);
        toggles.put("AutoFarm", true);
        toggles.put("AntiCheat", true);
        toggles.put("Unhook", true);

        values.put("AimSpeed", 0.2f);
        values.put("AimFOV", 90f);
        values.put("AimRange", 5f);
        values.put("TriggerCPS", 10f);
    }

    public static boolean isOn(String name) { return toggles.getOrDefault(name, true); }
    public static void toggle(String name) { toggles.put(name, !isOn(name)); }
    public static float get(String name) { return values.getOrDefault(name, 1f); }
    public static void set(String name, float val) { values.put(name, val); }
}
