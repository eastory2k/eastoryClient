package com.eastory.client.modules;

import com.eastory.client.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

public class TriggerBot extends ClientModule {
    private int cooldown;
    public TriggerBot() { super("TriggerBot"); }

    @Override
    public void onTick() {
        var p = EastoryClient.mc.player;
        if (p == null || p.isDead()) return;
        if (cooldown > 0) { cooldown--; return; }

        if (EastoryClient.mc.crosshairTarget instanceof EntityHitResult hit) {
            Entity e = hit.getEntity();
            if (e instanceof PlayerEntity && e.isAlive() && e != p && p.distanceTo(e) <= 3.2f) {
                EastoryClient.mc.interactionManager.attackEntity(p, e);
                p.swingHand(Hand.MAIN_HAND);
                cooldown = 2;
            }
        }
    }
}
