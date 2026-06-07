package com.eastory.client.modules;

import com.eastory.client.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import java.util.Random;

public class KillAura extends ClientModule {

    private final Random random = new Random();
    private int attackCooldown;
    private long lastAttackTime;
    private static final float MAX_ATTACK_DISTANCE = 3.0f;
    private static final float MAX_AIM_DISTANCE = 5.0f;

    public KillAura() { super("KillAura"); }

    @Override
    public void onTick() {
        var p = EastoryClient.mc.player;
        if (p == null || p.isDead()) return;

        LivingEntity target = findTarget();
        if (target == null) return;

        rotateToTarget(target);
        if (shouldAttack(target)) attack(target);
    }

    private LivingEntity findTarget() {
        var p = EastoryClient.mc.player;
        LivingEntity best = null;
        double bestDist = MAX_AIM_DISTANCE;

        for (Entity e : p.getWorld().getOtherEntities(p, p.getBoundingBox().expand(MAX_AIM_DISTANCE))) {
            if (!(e instanceof LivingEntity) || e == p || !e.isAlive()) continue;
            double d = p.distanceTo(e);
            if (d > MAX_AIM_DISTANCE) continue;

            Vec3d start = p.getEyePos();
            Vec3d end = e.getPos().add(0, e.getHeight() / 2, 0);
            HitResult hit = p.getWorld().raycast(new RaycastContext(start, end,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (hit.getType() != HitResult.Type.MISS) continue;

            if (best == null || d < bestDist) { best = (LivingEntity) e; bestDist = d; }
        }
        return best;
    }

    private void rotateToTarget(LivingEntity target) {
        var p = EastoryClient.mc.player;
        Vec3d aim = target.getPos().add(0, target.getHeight() * 0.8, 0);
        Vec3d eye = p.getEyePos();
        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));

        float gcd = getGCD();
        float sensitivity = 0.5f;
        float currentYaw = p.getYaw();
        float currentPitch = p.getPitch();
        float deltaYaw = MathHelper.wrapDegrees(yaw - currentYaw);
        float deltaPitch = pitch - currentPitch;

        float maxTurn = 30f;
        deltaYaw = MathHelper.clamp(deltaYaw, -maxTurn, maxTurn);
        deltaPitch = MathHelper.clamp(deltaPitch, -maxTurn, maxTurn);

        float noiseYaw = (random.nextFloat() - 0.5f) * 1.5f;
        float noisePitch = (random.nextFloat() - 0.5f) * 0.8f;

        p.setYaw(currentYaw + deltaYaw * sensitivity + noiseYaw);
        p.setPitch(MathHelper.clamp(currentPitch + deltaPitch * sensitivity + noisePitch, -90f, 90f));
        p.setHeadYaw(p.getYaw());
    }

    private float getGCD() {
        double sens = EastoryClient.mc.options.getMouseSensitivity().getValue();
        double value = sens * 0.6 + 0.2;
        return (float) (Math.pow(value, 3) * 0.8) * 0.15f;
    }

    private boolean shouldAttack(LivingEntity target) {
        if (attackCooldown > 0) { attackCooldown--; return false; }
        var p = EastoryClient.mc.player;
        if (p.distanceTo(target) > MAX_ATTACK_DISTANCE) return false;
        if (p.getAttackCooldownProgress(0.5f) < 0.9f) return false;

        long now = System.currentTimeMillis();
        float cps = Config.get("TriggerCPS");
        long delay = (cps > 0) ? (long)(1000L / cps) : 200;
        if (now - lastAttackTime < delay) return false;
        return true;
    }

    private void attack(LivingEntity target) {
        var p = EastoryClient.mc.player;
        var im = EastoryClient.mc.interactionManager;
        if (im == null) return;

        if (shouldAutoMace()) {
            int maceSlot = findMaceSlot();
            if (maceSlot != -1) {
                int prevSlot = p.getInventory().selectedSlot;
                p.getInventory().selectedSlot = maceSlot;
                im.attackEntity(p, target);
                p.swingHand(Hand.MAIN_HAND);
                p.getInventory().selectedSlot = prevSlot;
            } else {
                im.attackEntity(p, target);
                p.swingHand(Hand.MAIN_HAND);
            }
        } else {
            im.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
        }

        lastAttackTime = System.currentTimeMillis();
        attackCooldown = 20 / (int)Math.max(1, Config.get("TriggerCPS"));
    }

    private boolean shouldAutoMace() {
        var p = EastoryClient.mc.player;
        return p.fallDistance > 5.0f && !p.isOnGround() && p.getVelocity().y < -0.35;
    }

    private int findMaceSlot() {
        var p = EastoryClient.mc.player;
        for (int i = 0; i < 9; i++) if (p.getInventory().getStack(i).isOf(Items.MACE)) return i;
        return -1;
    }
}
