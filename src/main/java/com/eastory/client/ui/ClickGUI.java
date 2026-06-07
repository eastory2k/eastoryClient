package com.eastory.client.ui;

import com.eastory.client.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.*;

public class ClickGUI {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final TextRenderer f = mc.textRenderer;

    private boolean open;
    private int x = 150, y = 80, w = 250, h = 280;
    private int dragX, dragY;
    private boolean dragging;

    private int selectedCategory;
    private int selectedModule = -1;

    private static final String[] CATEGORIES = {"Combat", "Movement", "Visual", "Bypass", "Misc"};
    private static final String[][] MODULES = {
        {"KillAura", "TriggerBot", "AimAssist", "AutoSword"},
        {"FastPlace", "AutoFarm"},
        {},
        {"AntiCheat", "Unhook"},
        {}
    };
    private static final String[][] SETTINGS = {
        {"AimSpeed", "AimFOV", "AimRange", "TriggerCPS"},
        {},
        {},
        {},
        {}
    };

    public void open() {
        open = true;
        if (mc.player != null) mc.player.sendMessage(Text.literal("§aGUI opened"), true);
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() { return open; }

    public void tick() {
        if (!open) return;
        int mx = (int)(mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth());
        int my = (int)(mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight());

        if (mc.options.attackKey.isPressed()) {
            if (mx >= x && mx <= x + w && my >= y && my <= y + 22) {
                if (!dragging) { dragging = true; dragX = mx - x; dragY = my - y; }
            }
            if (dragging) { x = mx - dragX; y = my - dragY; }

            if (!dragging) {
                int catY = y + 25;
                for (int i = 0; i < CATEGORIES.length; i++) {
                    int cw = f.getWidth(CATEGORIES[i]) + 10;
                    if (mx >= x + 10 + i * 50 && mx <= x + 10 + i * 50 + cw && my >= catY && my <= catY + 14) {
                        selectedCategory = i; selectedModule = -1;
                    }
                }

                String[] mods = MODULES[selectedCategory];
                int modY = catY + 20;
                for (int i = 0; i < mods.length; i++) {
                    int my2 = modY + i * 18;
                    if (mx >= x + 12 && mx <= x + w - 12 && my >= my2 && my <= my2 + 14) {
                        selectedModule = i;
                    }
                }

                if (selectedModule >= 0 && mods.length > selectedModule) {
                    String modName = mods[selectedModule];
                    int toggleY = modY + mods.length * 18 + 10;
                    if (mx >= x + 12 && mx <= x + 40 && my >= toggleY && my <= toggleY + 14) {
                        Config.toggle(modName);
                    }
                }
            }
        } else {
            dragging = false;
        }
    }

    public void render(DrawContext c, float delta) {
        if (!open) return;

        int mx = (int)(mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth());
        int my = (int)(mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight());

        c.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x60000000);
        c.fillGradient(x, y, x + w, y + h, 0xDD0D0D0D, 0xDD0A0A0A);
        c.fillGradient(x, y, x + w, y + 3, 0xFF8B5CF6, 0xFF6D28D9);
        border(c, x, y, w, h, 0xFF2A2A2A);

        c.drawTextWithShadow(f, "EASTORY", x + w / 2 - f.getWidth("EASTORY") / 2, y + 7, 0xFF8B5CF6);

        int catY = y + 25;
        for (int i = 0; i < CATEGORIES.length; i++) {
            int col = i == selectedCategory ? 0xFF8B5CF6 : 0xFFAAAAAA;
            c.drawTextWithShadow(f, CATEGORIES[i], x + 10 + i * 50, catY, col);
        }
        c.fill(x + 10, catY + 15, x + w - 20, 1, 0xFF2A2A2A);

        if (selectedCategory >= 0) {
            String[] mods = MODULES[selectedCategory];
            int modY = catY + 22;
            for (int i = 0; i < mods.length; i++) {
                int my2 = modY + i * 18;
                if (my2 < y + 40 || my2 > y + h - 20) continue;
                boolean on = Config.isOn(mods[i]);
                int col = i == selectedModule ? 0xFF8B5CF6 : (on ? 0xFF34D399 : 0xFFEF4444);
                c.drawTextWithShadow(f, (on ? "● " : "○ ") + mods[i], x + 14, my2, col);
            }

            if (selectedModule >= 0 && selectedModule < mods.length) {
                String modName = mods[selectedModule];
                int setY = modY + mods.length * 18 + 8;
                if (setY > y + h - 40) return;
                c.fill(x + 10, setY, x + w - 20, 1, 0xFF2A2A2A);
                setY += 6;

                boolean on = Config.isOn(modName);
                c.drawTextWithShadow(f, modName, x + 14, setY, 0xFFFFFFFF);
                c.drawTextWithShadow(f, on ? "§aON" : "§cOFF", x + w - 30, setY, on ? 0xFF34D399 : 0xFFEF4444);
                setY += 14;

                String[] sets = SETTINGS[selectedCategory];
                if (sets != null) {
                    for (String s : sets) {
                        if (setY > y + h - 20) break;
                        float val = Config.get(s);
                        c.drawTextWithShadow(f, s + ": " + String.format("%.1f", val), x + 14, setY, 0xFFCCCCCC);

                        int barX = x + w - 80, barY = setY + 4, barW = 70;
                        c.fill(barX, barY, barX + barW, barY + 6, 0xFF333333);
                        float max = s.equals("AimFOV") ? 180 : s.equals("TriggerCPS") ? 20 : 10;
                        float pct = val / max;
                        c.fill(barX, barY, barX + (int)(barW * pct), barY + 6, 0xFF8B5CF6);

                        if (mx >= barX && mx <= barX + barW && my >= barY && my <= barY + 6 && mc.options.attackKey.isPressed()) {
                            float newVal = (float)(mx - barX) / barW * max;
                            Config.set(s, Math.max(0, Math.min(max, newVal)));
                        }
                        setY += 16;
                    }
                }
            }
        }
    }

    private void border(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col);
        c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y, x + 1, y + h, col);
        c.fill(x + w - 1, y, x + w, y + h, col);
    }
}
