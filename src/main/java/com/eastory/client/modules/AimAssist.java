package com.eastory.client.modules;

import com.eastory.client.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import java.util.Random;

public class AimAssist extends ClientModule {

    private final Random random = new Random();
    private float lastYaw, lastPitch;
    private boolean appliedLastTick;
    private long hesitationUntil;
    private long nextPointAt;
    private Vec3d pointOffset = Vec3d.ZERO;

    private static final float MOTOR_NOISE = 0.12f;
    private static final float AIM_SPEED = 20.0f;
    private static final float SMOOTH_VALUE = 5.0f;
    private static final boolean HUMAN_AIM = true;
    private static final boolean VERTICAL_AIM = true;
    private static final boolean GENERATE_HESITATIONS = true;
    private static final boolean MULTIPOINT = true;
    private static final boolean YIELD_TO_MOUSE = true;
    private static final boolean INPUT_BASED = true;
    private static final float INPUT_THRESHOLD = 0.6f;
    private static final int PREDICTION_TICKS = 2;

    public AimAssist() { super("AimAssist"); }

    @Override
    public void onTick() {
        var p = EastoryClient.mc.player;
        if (p == null || p.isDead()) {
            appliedLastTick = false;
            return;
        }

        Entity target = findTarget();
        if (target == null) {
            appliedLastTick = false;
            return;
        }

        Vec3d aimPoint = getAimPoint(target);
        Vec3d eyes = p.getEyePos();
        double dx = aimPoint.x - eyes.x;
        double dy = aimPoint.y - eyes.y;
        double dz = aimPoint.z - eyes.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float targetPitch = VERTICAL_AIM ? (float) -Math.toDegrees(Math.atan2(dy, horizontal)) : p.getPitch();

        float yawDelta = MathHelper.wrapDegrees(targetYaw - p.getYaw());
        float pitchDelta = targetPitch - p.getPitch();
        float angle = (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);

        if (angle < 0.01f || shouldPause(yawDelta, pitchDelta)) return;

        float[] adjusted = getAdjustedDelta(yawDelta, pitchDelta, angle);
        float nextYaw = p.getYaw() + adjusted[0];
        float nextPitch = MathHelper.clamp(p.getPitch() + adjusted[1], -90f, 90f);

        p.setYaw(nextYaw);
        p.setPitch(nextPitch);
        p.setHeadYaw(nextYaw);

        lastYaw = nextYaw;
        lastPitch = nextPitch;
        appliedLastTick = true;
    }

    private Entity findTarget() {
        var p = EastoryClient.mc.player;
        Entity best = null;
        double bestDist = 5.0;

        for (Entity e : p.getWorld().getEntities()) {
            if (!(e instanceof PlayerEntity) || e == p || !e.isAlive()) continue;
            double d = p.distanceTo(e);
            Vec3d look = p.getRotationVector();
            Vec3d to = e.getPos().add(0, e.getHeight() / 2, 0).subtract(p.getEyePos()).normalize();
            double ang = Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(to), -1, 1)));
            if (d < bestDist && ang < 90) { bestDist = d; best = e; }
        }
        return best;
    }

    private Vec3d getAimPoint(Entity target) {
        Vec3d base = target.getPos().add(0, target.getHeight() * (VERTICAL_AIM ? 0.55f : 0.5f), 0);
        Vec3d vel = target.getVelocity().multiply(PREDICTION_TICKS);
        base = base.add(vel);

        if (MULTIPOINT) {
            long now = System.nanoTime();
            if (now >= nextPointAt) {
                pointOffset = new Vec3d(
                    (random.nextDouble() - 0.5) * 0.7,
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.7
                );
                nextPointAt = now + 25_000_000L;
            }
            base = base.add(pointOffset);
        }
        return base;
    }

    private boolean shouldPause(float yawDelta, float pitchDelta) {
        if (appliedLastTick) {
            float manualYaw = MathHelper.wrapDegrees(EastoryClient.mc.player.getYaw() - lastYaw);
            float manualPitch = EastoryClient.mc.player.getPitch() - lastPitch;
            float manualAmount = Math.abs(manualYaw) + Math.abs(manualPitch);

            if (INPUT_BASED && manualAmount < INPUT_THRESHOLD) return true;
            if (YIELD_TO_MOUSE && (Math.signum(manualYaw) == -Math.signum(yawDelta) || Math.signum(manualPitch) == -Math.signum(pitchDelta))
                && manualAmount > INPUT_THRESHOLD) return true;
        }

        if (GENERATE_HESITATIONS) {
            long now = System.currentTimeMillis();
            if (now < hesitationUntil) return true;
            if (random.nextFloat() < 0.018f) {
                hesitationUntil = now + 45 + random.nextInt(115);
                return true;
            }
        }
        return false;
    }

    private float[] getAdjustedDelta(float yawDelta, float pitchDelta, float angle) {
        float speed = AIM_SPEED * 0.35f;
        float smoothing = MathHelper.clamp(20f / Math.max(SMOOTH_VALUE, 1f), 0.1f, 1f);
        float factor = MathHelper.clamp(angle / 45f, 0.08f, 1f) * smoothing;

        if (HUMAN_AIM) {
            speed *= 0.7f + 0.3f * factor;
            factor *= 0.75f + random.nextFloat() * 0.2f;
        }

        float yawStep = MathHelper.clamp(yawDelta * factor, -speed, speed);
        float pitchStep = VERTICAL_AIM ? MathHelper.clamp(pitchDelta * factor, -speed, speed) : 0f;

        if (MOTOR_NOISE > 0f) {
            yawStep += (random.nextFloat() - 0.5f) * MOTOR_NOISE;
            pitchStep += (random.nextFloat() - 0.5f) * MOTOR_NOISE * 0.65f;
        }

        return new float[]{yawStep, pitchStep};
    }
}
