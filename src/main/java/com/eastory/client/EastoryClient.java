package com.eastory.client;

import com.eastory.client.modules.*;
import com.eastory.client.bypass.*;
import com.eastory.client.ui.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class EastoryClient implements ClientModInitializer {

    public static MinecraftClient mc;
    public static boolean guiHidden;
    public static boolean permanentlyHidden;
    private static ClientModule[] modules;
    private static HudRenderer hud;
    private static ClickGUI gui;
    private static boolean rshiftWasPressed;

    @Override
    public void onInitializeClient() {
        mc = MinecraftClient.getInstance();

        modules = new ClientModule[] {
            new KillAura(),
            new TriggerBot(),
            new AimAssist(),
            new AutoSword(),
            new FastPlace(),
            new AutoFarm(),
            new AnticheatBypass(),
            new UnhookModule()
        };

        modules[0].keyBinding = registerKey("KillAura", GLFW.GLFW_KEY_F6);
        modules[1].keyBinding = registerKey("TriggerBot", GLFW.GLFW_KEY_F7);
        modules[2].keyBinding = registerKey("AimAssist", GLFW.GLFW_KEY_F8);
        modules[3].keyBinding = registerKey("AutoSword", GLFW.GLFW_KEY_F9);
        modules[4].keyBinding = registerKey("FastPlace", GLFW.GLFW_KEY_F10);
        modules[5].keyBinding = registerKey("AutoFarm", GLFW.GLFW_KEY_F11);
        modules[6].keyBinding = registerKey("AntiCheat", GLFW.GLFW_KEY_F12);
        modules[7].keyBinding = registerKey("Unhook", GLFW.GLFW_KEY_HOME);

        hud = new HudRenderer();
        gui = new ClickGUI();

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            for (ClientModule m : modules) {
                if (m.keyBinding != null && m.keyBinding.wasPressed()) {
                    m.enabled = !m.enabled;
                    client.player.sendMessage(Text.literal((m.enabled ? "§a" : "§c") + m.name + (m.enabled ? " ON" : " OFF")), true);
                }
            }

            if (!permanentlyHidden) {
                boolean rshift = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                if (rshift && !rshiftWasPressed) {
                    if (gui.isOpen()) {
                        gui.close();
                        permanentlyHidden = true;
                        if (client.player != null) client.player.sendMessage(Text.literal("§cGUI permanently hidden"), false);
                    } else {
                        gui.open();
                    }
                }
                rshiftWasPressed = rshift;
            }

            if (gui.isOpen()) {
                gui.tick();
            } else {
                for (ClientModule m : modules) if (m.enabled) m.onTick();
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (mc.player == null || mc.currentScreen != null) return;
            if (gui.isOpen()) {
                gui.render(drawContext, tickCounter);
            } else if (!permanentlyHidden && !guiHidden) {
                hud.render(drawContext, mc.textRenderer, modules);
            }
        });
    }

    private static KeyBinding registerKey(String name, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.eastory." + name.toLowerCase(),
            InputUtil.Type.KEYSYM,
            defaultKey,
            "Eastory Client"
        ));
    }
}
