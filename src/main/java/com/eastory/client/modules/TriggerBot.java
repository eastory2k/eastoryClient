package com.eastory.client.modules;

import com.eastory.client.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

public class TriggerBot extends ClientModule {

    private int cooldown;
    private long lastAttackTime;

    public TriggerBot() { super("TriggerBot"); }

    @Override
    public void onTick() {
        var p = EastoryClient.mc.player;
        if (p == null || p.isDead()) return;
        if (cooldown > 0) { cooldown--; return; }

        if (!(EastoryClient.mc.crosshairTarget instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof PlayerEntity target) || !target.isAlive() || target == p) return;
        if (p.distanceTo(target) > 3.2f) return;

        if (p.getAttackCooldownProgress(0.5f) < 0.9f) return;

        long now = System.currentTimeMillis();
        float cps = Config.get("TriggerCPS");
        long delay = (cps > 0) ? (long)(1000L / cps) : 200;
        if (now - lastAttackTime < delay) return;

        EastoryClient.mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);

        lastAttackTime = now;
        cooldown = Math.max(1, (int)(20.0 / cps));
    }
}
