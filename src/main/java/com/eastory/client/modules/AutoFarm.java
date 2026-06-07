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

public class AutoFarm extends ClientModule {

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

    private void handleSword() {
        if (!ensureHotbarSword()) return;
        if (System.currentTimeMillis() - lastSell > 60_000) {
            mc.player.networkHandler.sendChatCommand("ah sell 15000");
            lastSell = System.currentTimeMillis();
        }
    }

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

    private boolean isDirt(BlockPos pos) {
        Block b = mc.world.getBlockState(pos).getBlock();
        return b == Blocks.DIRT || b == Blocks.GRASS_BLOCK;
    }

    private BlockPos findAppleDirt() {
        return BlockPos.stream(mc.player.getBlockPos().add(-4, -2, -4), mc.player.getBlockPos().add(4, 2, 4))
                .filter(this::isDirt).filter(p -> mc.world.getBlockState(p.up()).isAir())
                .min(Comparator.comparingDouble(p -> p.getSquaredDistance(mc.player.getPos())))
                .orElse(null);
    }

    private BlockPos findTreeBlock(BlockPos root) {
        return BlockPos.stream(root.add(-4, 0, -4), root.add(4, 8, 4))
                .filter(p -> mc.world.getBlockState(p).isIn(BlockTags.LOGS) || mc.world.getBlockState(p).isIn(BlockTags.LEAVES))
                .min(Comparator.comparingDouble(p -> p.getSquaredDistance(mc.player.getPos())))
                .orElse(null);
    }

    private boolean ensureHotbar(Item item) {
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).getItem() == item) { inv.selectedSlot = i; return true; }
        }
        return false;
    }

    private boolean ensureHotbarAxeOrHoe() {
        return ensureHotbar(Items.IRON_AXE) || ensureHotbar(Items.DIAMOND_AXE) || ensureHotbar(Items.NETHERITE_AXE)
                || ensureHotbar(Items.IRON_HOE) || ensureHotbar(Items.DIAMOND_HOE) || ensureHotbar(Items.NETHERITE_HOE);
    }

    private boolean ensureHotbarSword() {
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).getItem() instanceof SwordItem) { inv.selectedSlot = i; return true; }
        }
        return false;
    }

    private void useBlock(BlockPos pos, Direction dir) {
        Vec3d hit = Vec3d.ofCenter(pos).add(dir.getOffsetX() * 0.5, dir.getOffsetY() * 0.5, dir.getOffsetZ() * 0.5);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, dir, pos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void breakBlock(BlockPos pos) {
        mc.interactionManager.attackBlock(pos, Direction.UP);
        mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isInventoryFull() { return mc.player.getInventory().getEmptySlot() == -1; }

    private boolean hasCropItems() {
        Item[] crops = {Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.NETHER_WART, Items.SUGAR_CANE};
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack s = mc.player.getInventory().main.get(i);
            for (Item c : crops) if (s.getItem() == c) return true;
        }
        return false;
    }

    private void depositCrops() {
        ChestBlockEntity chest = findBlockEntity(ChestBlockEntity.class, 5);
        if (chest != null) {
            BlockPos pos = chest.getPos();
            if (!isInRange(pos)) walkTo(pos.toCenterPos(), 1.0);
            else {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false));
                if (actionTimer.elapsed(500)) { mc.player.closeHandledScreen(); actionTimer.reset(); }
            }
        }
    }

    private boolean pickupNearbyItems() {
        List<ItemEntity> items = mc.world.getEntitiesByClass(ItemEntity.class, mc.player.getBoundingBox().expand(6),
                e -> isCropItem(e.getStack().getItem()));
        if (items.isEmpty()) return false;
        ItemEntity nearest = items.stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player))).get();
        Vec3d pos = nearest.getPos();
        if (nearest.squaredDistanceTo(mc.player) > 2.25) walkTo(pos, 2.25);
        else stopWalking();
        return true;
    }

    private boolean isCropItem(Item item) {
        return item == Items.WHEAT || item == Items.CARROT || item == Items.POTATO || item == Items.BEETROOT
                || item == Items.NETHER_WART || item == Items.SUGAR_CANE;
    }

    private boolean tryReplant() {
        if (pendingReplant == null) return false;
        Item seed = Items.WHEAT_SEEDS;
        BlockPos target = pendingReplant;
        if (!isInRange(target)) { walkTo(target.toCenterPos(), 1.0); return true; }
        stopWalking();
        if (!mc.world.getBlockState(target).isAir()) { pendingReplant = null; return false; }
        if (actionTimer.elapsed(80)) {
            ensureHotbar(seed);
            useBlock(target.down(), Direction.UP);
            pendingReplant = null;
            actionTimer.reset();
        }
        return true;
    }

    private void tryReplantNearPlayer() {
        Item seed = Items.WHEAT_SEEDS;
        BlockPos origin = mc.player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -24; x <= 24; x++) {
            for (int z = -24; z <= 24; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = origin.add(x, y, z);
                    if (isPlantable(pos, seed)) {
                        double d = pos.getSquaredDistance(mc.player.getPos());
                        if (d < bestDist) { bestDist = d; best = pos; }
                    }
                }
            }
        }
        if (best != null) {
            if (!isInRange(best)) walkTo(best.toCenterPos(), 1.0);
            else { stopWalking(); ensureHotbar(seed); useBlock(best.down(), Direction.UP); actionTimer.reset(); }
        }
    }

    private boolean isPlantable(BlockPos pos, Item seed) {
        if (!mc.world.getBlockState(pos).isAir()) return false;
        Block below = mc.world.getBlockState(pos.down()).getBlock();
        return below == Blocks.FARMLAND || below == Blocks.SOUL_SAND || below == Blocks.SAND || below == Blocks.DIRT || below == Blocks.GRASS_BLOCK;
    }

    private BlockPos findReadyCrop() {
        BlockPos origin = mc.player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -24; x <= 24; x++) {
            for (int z = -24; z <= 24; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = origin.add(x, y, z);
                    if (isCropReady(pos)) {
                        double d = pos.getSquaredDistance(mc.player.getPos());
                        if (d < bestDist) { bestDist = d; best = pos; }
                    }
                }
            }
        }
        return best;
    }

    private boolean isCropReady(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.WHEAT) return state.get(CropBlock.AGE) >= CropBlock.MAX_AGE;
        if (block == Blocks.CARROTS) return state.get(CropBlock.AGE) >= CropBlock.MAX_AGE;
        if (block == Blocks.POTATOES) return state.get(CropBlock.AGE) >= CropBlock.MAX_AGE;
        if (block == Blocks.BEETROOTS) return state.get(CropBlock.AGE) >= CropBlock.MAX_AGE;
        if (block == Blocks.NETHER_WART) return state.get(NetherWartBlock.AGE) >= 3;
        if (block == Blocks.SUGAR_CANE) return mc.world.getBlockState(pos.down()).getBlock() == Blocks.SUGAR_CANE;
        return false;
    }

    private boolean tryBoneMeal() {
        if (!hasItem(Items.BONE_MEAL)) return false;
        BlockPos target = findBoneMealTarget();
        if (target == null) return false;
        if (!isInRange(target)) { walkTo(target.toCenterPos(), 1.0); return true; }
        stopWalking();
        if (actionTimer.elapsed(35)) {
            ensureHotbar(Items.BONE_MEAL);
            useBlock(target, Direction.UP);
            actionTimer.reset();
        }
        return true;
    }

    private BlockPos findBoneMealTarget() {
        BlockPos origin = mc.player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -24; x <= 24; x++) {
            for (int z = -24; z <= 24; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = origin.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop && crop.getAge(state) < CropBlock.MAX_AGE) {
                        double d = pos.getSquaredDistance(mc.player.getPos());
                        if (d < bestDist) { bestDist = d; best = pos; }
                    }
                }
            }
        }
        return best;
    }

    private boolean hasItem(Item item) {
        return mc.player.getInventory().main.stream().anyMatch(s -> s.getItem() == item);
    }

    private boolean isInRange(BlockPos pos) {
        return mc.player.squaredDistanceTo(pos.toCenterPos()) <= 6.25;
    }

    private void walkTo(Vec3d target, double stopRangeSq) {
        Vec3d delta = target.subtract(mc.player.getPos());
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90F;
        mc.player.setYaw(yaw);
        mc.player.setPitch(0);
        mc.options.forwardKey.setPressed(true);
        mc.options.sprintKey.setPressed(true);
        mc.options.jumpKey.setPressed(mc.player.horizontalCollision && mc.player.isOnGround());
        cropWalkTarget = target;
        cropWalking = true;
    }

    private void stopWalking() {
        mc.options.forwardKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        cropWalkTarget = null;
        cropWalking = false;
    }

    private <T extends BlockEntity> T findBlockEntity(Class<T> clazz, double range) {
        BlockPos origin = mc.player.getBlockPos();
        int r = (int) Math.ceil(range);
        T best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.add(x, y, z);
                    BlockEntity be = mc.world.getBlockEntity(pos);
                    if (clazz.isInstance(be)) {
                        double d = pos.getSquaredDistance(mc.player.getPos());
                        if (d < bestDist) { bestDist = d; best = clazz.cast(be); }
                    }
                }
            }
        }
        return best;
    }

    private static class Timer {
        long start = System.currentTimeMillis();
        boolean elapsed(long ms) { return System.currentTimeMillis() - start >= ms; }
        void reset() { start = System.currentTimeMillis(); }
    }
}
