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

    private static final int BG = 0xCC0D0D0D, ACCENT = 0xFF6BB5FF;
    private static final int GREEN = 0xFF55FF55, RED = 0xFFFF5555, WHITE = 0xFFFFFFFF;
    private final SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
    private float fps;
    private long last;
    private int idx;
    private final float[] buf = new float[10];

    public void render(DrawContext c, TextRenderer f, Module[] modules) {
        var p = EastoryClient.mc.player;
        var w = EastoryClient.mc.world;
        if (p == null || w == null) return;

        int sw = EastoryClient.mc.getWindow().getScaledWidth();
        int sh = EastoryClient.mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();

        if (last > 0) {
            buf[idx++ % buf.length] = 1000f / (now - last);
            float s = 0; for (float v : buf) s += v;
            fps = s / buf.length;
        }
        last = now;

        // левая панель модулей
        int lw = 90, lh = 30 + modules.length * 13;
        c.fill(5, 5, 5 + lw, 5 + lh, BG);
        c.fill(5, 5, 5 + lw, 7, ACCENT);

        int y = 12;
        c.drawTextWithShadow(f, "EASTORY", 5 + lw / 2 - f.getWidth("EASTORY") / 2, y, ACCENT); y += 13;
        for (Module m : modules) {
            int col = m.enabled ? GREEN : RED;
            c.drawTextWithShadow(f, (m.enabled ? "● " : "○ ") + m.name, 12, y, col);
            y += 13;
        }

        // правая панель — список игроков
        var players = new ArrayList<PlayerEntity>();
        for (var e : w.getEntities()) {
            if (e instanceof PlayerEntity pl && pl != p) players.add(pl);
        }
        players.sort((a, b) -> Float.compare(p.distanceTo(a), p.distanceTo(b)));

        if (!players.isEmpty()) {
            int rx = sw - 135, ry = 5, rw = 130, max = Math.min(players.size(), 8), rh = 22 + max * 18;
            c.fill(rx, ry, rx + rw, ry + rh, BG);

            int py = ry + 7;
            c.drawTextWithShadow(f, "PLAYERS", rx + rw / 2 - f.getWidth("PLAYERS") / 2, py, ACCENT); py += 13;

            for (int i = 0; i < max; i++) {
                var pl = players.get(i);
                float hp = (pl.getHealth() + pl.getAbsorptionAmount()) / pl.getMaxHealth();
                int col = hp > 0.6f ? GREEN : hp > 0.3f ? 0xFFFFFF55 : RED;
                String n = pl.getName().getString();
                if (n.length() > 14) n = n.substring(0, 11) + "..";
                c.drawTextWithShadow(f, n, rx + 5, py, col);
                c.drawTextWithShadow(f, (int)(hp * 100) + "%", rx + rw - 5 - f.getWidth((int)(hp * 100) + "%"), py, col);
                c.fill(rx + 5, py + 11, rx + 5 + (int)((rw - 10) * hp), py + 14, col);
                py += 18;
            }
        }

        // статус-бар
        int sy = sh - 14;
        c.fill(0, sy, sw, sy + 14, 0xEE000000);
        c.fill(0, sy, sw, sy + 2, ACCENT);

        int ping = 0;
        if (EastoryClient.mc.getNetworkHandler() != null) {
            PlayerListEntry e = EastoryClient.mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String server = EastoryClient.mc.getCurrentServerEntry() != null ? EastoryClient.mc.getCurrentServerEntry().address : "SP";
        c.drawTextWithShadow(f, "Eastory | " + (int)fps + " FPS | " + ping + "ms | " + server, 5, sy + 3, WHITE);

        String time = tf.format(new Date());
        c.drawTextWithShadow(f, time, sw - 5 - f.getWidth(time), sy + 3, ACCENT);
    }
}
