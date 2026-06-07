package com.eastory.client.modules;

import com.eastory.client.*;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

public class AutoSword extends ClientModule {
    private int lastSlot = -1;

    public AutoSword() { super("AutoSword"); }

    @Override
    public void onTick() {
        var p = EastoryClient.mc.player;
        if (p == null) return;

        if (EastoryClient.mc.options.attackKey.isPressed()) {
            int best = -1;
            float maxDmg = 0;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = p.getInventory().getStack(i);
                float dmg = getDamage(stack);
                if (dmg > maxDmg) { maxDmg = dmg; best = i; }
            }
            if (best >= 0 && p.getInventory().selectedSlot != best) {
                if (lastSlot < 0) lastSlot = p.getInventory().selectedSlot;
                p.getInventory().selectedSlot = best;
            }
        } else if (lastSlot >= 0) {
            p.getInventory().selectedSlot = lastSlot;
            lastSlot = -1;
        }
    }

    private float getDamage(ItemStack stack) {
        if (stack.isOf(Items.NETHERITE_SWORD)) return 8.0f;
        if (stack.isOf(Items.DIAMOND_SWORD))    return 7.0f;
        if (stack.isOf(Items.IRON_SWORD))       return 6.0f;
        if (stack.isOf(Items.STONE_SWORD))      return 5.0f;
        if (stack.isOf(Items.WOODEN_SWORD))     return 4.0f;
        if (stack.isOf(Items.GOLDEN_SWORD))     return 4.0f;
        if (stack.isOf(Items.TRIDENT))          return 9.0f;
        if (stack.isOf(Items.MACE))             return 7.0f;
        return 1.0f;
    }
}
