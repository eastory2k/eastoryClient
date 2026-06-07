package com.eastory.client;

import com.eastory.client.modules.*;
import com.eastory.client.bypass.*;
import com.eastory.client.ui.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class EastoryClient implements ClientModInitializer {

    public static MinecraftClient mc;
    public static boolean guiHidden;
    private static ClientModule[] modules;
    private static HudRenderer hud;
    private static boolean rshiftWasPressed;

    @Override
    public void onInitializeClient() {
        mc = MinecraftClient.getInstance();

        modules = new ClientModule[] {
            new KillAura(),
            new AimAssist(),
            new TriggerBot(),
            new AutoSword(),
            new FastPlace(),
            new AutoFarm(),
            new AnticheatBypass(),
            new UnhookModule()
        };

        hud = new HudRenderer();

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            for (ClientModule m : modules) {
                if (m.enabled) m.onTick();
            }

            boolean rshift = org.lwjgl.glfw.GLFW.glfwGetKey(
                mc.getWindow().getHandle(),
                org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT
            ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

            if (rshift && !rshiftWasPressed) {
                guiHidden = !guiHidden;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(guiHidden ? "§cGUI hidden" : "§aGUI shown"), true);
                }
            }
            rshiftWasPressed = rshift;
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (mc.player == null || guiHidden || mc.currentScreen != null) return;
            hud.render(drawContext, mc.textRenderer, modules);
        });
    }
}
