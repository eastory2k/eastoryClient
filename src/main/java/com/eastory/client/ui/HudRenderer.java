package com.eastory.client.ui;

import com.eastory.client.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import java.text.SimpleDateFormat;
import java.util.*;

public class HudRenderer {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final TextRenderer f = mc.textRenderer;
    private static final SimpleDateFormat tf = new SimpleDateFormat("HH:mm");

    private static final int BG = 0xAA0A0A0A;
    private static final int ACCENT = 0xFF8B5CF6;
    private static final int GREEN = 0xFF34D399;
    private static final int RED = 0xFFEF4444;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF9CA3AF;
    private static final int YELLOW = 0xFFFBBF24;

    private float fps;
    private long last;
    private int idx;
    private final float[] buf = new float[10];

    public void render(DrawContext c, TextRenderer f, ClientModule[] modules) {
        var p = EastoryClient.mc.player;
        var w = EastoryClient.mc.world;
        if (p == null || w == null) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();

        if (last > 0) {
            buf[idx++ % buf.length] = 1000f / (now - last);
            float s = 0; for (float v : buf) s += v;
            fps = s / buf.length;
        }
        last = now;

        int lx = 6, ly = 6, lw = 125, lh = 42 + modules.length * 16;
        c.fill(lx + 2, ly + 2, lx + lw + 2, ly + lh + 2, 0x40000000);
        c.fillGradient(lx, ly, lx + lw, ly + lh, 0xDD0D0D0D, 0xDD0A0A0A);
        c.fillGradient(lx, ly, lx + lw, ly + 3, ACCENT, 0xFF6D28D9);
        border(c, lx, ly, lw, lh, 0xFF1F1F1F);

        int y = ly + 12;
        c.drawTextWithShadow(f, "EASTORY", lx + lw / 2 - f.getWidth("EASTORY") / 2, y, ACCENT); y += 15;
        c.fill(lx + 8, y, lx + lw - 8, y, 0xFF2A2A2A); y += 6;

        for (ClientModule m : modules) {
            boolean on = m.enabled;
            c.fill(lx + 10, y + 2, lx + 18, y + 10, on ? GREEN : RED);
            c.drawTextWithShadow(f, m.name, lx + 24, y, on ? WHITE : GRAY);
            if (m.keyBinding != null) {
                String key = m.keyBinding.getBoundKeyLocalizedText().getString();
                if (!key.isEmpty()) {
                    int kw = f.getWidth(key);
                    c.fill(lx + lw - 10 - kw - 3, y, lx + lw - 10 + 3, y + 10, 0xFF2A2A2A);
                    c.drawTextWithShadow(f, key, lx + lw - 10 - kw, y + 1, GRAY);
                }
            }
            y += 16;
        }

        var players = new ArrayList<PlayerEntity>();
        for (var e : w.getEntities()) {
            if (e instanceof PlayerEntity pl && pl != p) players.add(pl);
        }
        players.sort((a, b) -> Float.compare(p.distanceTo(a), p.distanceTo(b)));

        if (!players.isEmpty()) {
            int rx = sw - 138, ry = 6, rw = 132, max = Math.min(players.size(), 6), rh = 34 + max * 22;
            c.fill(rx + 2, ry + 2, rx + rw + 2, ry + rh + 2, 0x40000000);
            c.fillGradient(rx, ry, rx + rw, ry + rh, 0xDD0D0D0D, 0xDD0A0A0A);
            c.fillGradient(rx, ry, rx + rw, ry + 3, ACCENT, 0xFF6D28D9);
            border(c, rx, ry, rw, rh, 0xFF1F1F1F);

            int py = ry + 10;
            c.drawTextWithShadow(f, "PLAYERS", rx + rw / 2 - f.getWidth("PLAYERS") / 2, py, ACCENT); py += 13;
            c.fill(rx + 8, py, rx + rw - 8, py, 0xFF2A2A2A); py += 5;

            for (int i = 0; i < max; i++) {
                var pl = players.get(i);
                float hp = (pl.getHealth() + pl.getAbsorptionAmount()) / pl.getMaxHealth();
                int col = hp > 0.6f ? GREEN : hp > 0.3f ? YELLOW : RED;
                String n = pl.getName().getString();
                if (n.length() > 11) n = n.substring(0, 8) + "..";
                c.drawTextWithShadow(f, n, rx + 8, py, WHITE);
                String hpText = (int)(hp * 100) + "%";
                c.drawTextWithShadow(f, hpText, rx + rw - 8 - f.getWidth(hpText), py, col);
                int barX = rx + 8, barY = py + 11, barW = rw - 16, barH = 4;
                c.fill(barX, barY, barX + barW, barY + barH, 0xFF1F1F1F);
                int fillW = (int)(barW * hp);
                if (fillW > 0) {
                    c.fillGradient(barX, barY, barX + fillW, barY + barH,
                        hp > 0.5f ? GREEN : (hp > 0.25f ? YELLOW : RED),
                        hp > 0.5f ? 0xFF2ECC71 : (hp > 0.25f ? YELLOW : 0xFFE74C3C));
                }
                py += 22;
            }
        }

        String watermark = "eastory client";
        c.drawTextWithShadow(f, watermark, sw - f.getWidth(watermark) - 5, sh - 14, 0x18FFFFFF);

        String fpsText = (int)fps + " fps";
        int fpsColor = fps > 60 ? GREEN : fps > 30 ? YELLOW : RED;
        c.drawTextWithShadow(f, fpsText, sw - f.getWidth(fpsText) - 5, sh - 30, fpsColor);
    }

    private void border(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col);
        c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y, x + 1, y + h, col);
        c.fill(x + w - 1, y, x + w, y + h, col);
    }
}
