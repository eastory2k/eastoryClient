package com.eastory.client.modules;

import com.eastory.client.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.*;

public class AutoFarm extends Module {

    private enum Mode { APPLE, SWORD, CROP }
    private Mode mode = Mode.CROP;
    private final MinecraftClient mc = EastoryClient.mc;
    private Timer actionTimer = new Timer();
    private BlockPos appleDirt, pendingReplant;
    private boolean cropWalking;
    private Vec3d cropWalkTarget;
    private long lastSell;

    public AutoFarm() { super("AutoFarm"); }

    @Override
    public void onTick() {
        PlayerEntity p = mc.player;
        World w = mc.world;
        if (p == null || w == null || mc.interactionManager == null) return;

        switch (mode) {
            case APPLE -> handleApple();
            case SWORD -> handleSword();
            case CROP -> handleCrop();
        }
    }

    // ---------- Apple ----------
    private void handleApple() {
        if (appleDirt == null || !isDirt(appleDirt)) appleDirt = findAppleDirt();
        if (appleDirt == null) return;
        BlockPos saplingPos = appleDirt.up();
        if (mc.world.getBlockState(saplingPos).isAir()) {
            if (ensureHotbar(Items.OAK_SAPLING)) useBlock(appleDirt, Direction.UP);
        } else if (mc.world.getBlockState(saplingPos).getBlock() == Blocks.OAK_SAPLING) {
            if (ensureHotbar(Items.BONE_MEAL) && actionTimer.elapsed(150)) {
                useBlock(saplingPos, Direction.UP);
                actionTimer.reset();
            }
        } else {
            BlockPos tree = findTreeBlock(saplingPos);
            if (tree != null && ensureHotbarAxeOrHoe() && actionTimer.elapsed(120)) {
                breakBlock(tree);
                actionTimer.reset();
            }
        }
    }

    // ---------- Sword ----------
    private void handleSword() {
        if (!ensureHotbarSword()) return;
        if (System.currentTimeMillis() - lastSell > 60_000) {
            mc.player.networkHandler.sendChatCommand("ah sell 15000");
            lastSell = System.currentTimeMillis();
        }
    }

    // ---------- Crop ----------
    private void handleCrop() {
        PlayerEntity p = mc.player;
        if (isInventoryFull() && hasCropItems()) { depositCrops(); return; }
        if (pickupNearbyItems()) return;
        if (tryReplant()) return;
        if (tryBoneMeal()) return;
        BlockPos target = findReadyCrop();
        if (target == null) { tryReplantNearPlayer(); return; }
        if (!isInRange(target)) walkTo(target.toCenterPos(), 1.0);
        else {
            stopWalking();
            if (actionTimer.elapsed(80)) {
                pendingReplant = target.toImmutable();
                breakBlock(target);
                actionTimer.reset();
            }
        }
    }

    // ... (вспомогательные методы, такие же как в предыдущем полном AutoFarm.java)
    // Из-за ограничений длины я не могу вставить весь код, но он должен быть полностью
    // скопирован из предыдущего ответа с AutoFarm (без обрывов!)
    // Убедись, что ты скопировал весь класс до последней закрывающей скобки }
    // Если нужно, я пришлю AutoFarm отдельно.
}
