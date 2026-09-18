package dev.veyra.client.compat;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Reads enchantments directly when Lunar's helper cannot resolve packet NBT. */
final class BwhItemEnchantments {
    static final int PROTECTION_ID = 0;
    static final int POWER_ID = 48;
    static final int PUNCH_ID = 49;

    private BwhItemEnchantments() {
    }

    static int level(ItemStack stack, Enchantment enchantment, int legacyId) {
        if (stack == null) return 0;
        int helperLevel = enchantment == null ? 0
                : EnchantmentHelper.getEnchantmentLevel(enchantment.effectId, stack);
        int rawLevel = 0;
        NBTTagList enchantments = stack.getEnchantmentTagList();
        if (enchantments != null) {
            for (int index = 0; index < enchantments.tagCount(); index++) {
                NBTTagCompound entry = enchantments.getCompoundTagAt(index);
                if ((entry.getShort("id") & 0xFFFF) == legacyId) {
                    rawLevel = Math.max(rawLevel, entry.getShort("lvl") & 0xFFFF);
                }
            }
        }
        return Math.max(helperLevel, rawLevel);
    }

    static int count(ItemStack stack) {
        if (stack == null) return 0;
        NBTTagList enchantments = stack.getEnchantmentTagList();
        return enchantments == null ? 0 : enchantments.tagCount();
    }

    static String itemData(ItemStack stack) {
        if (stack == null) return "";
        NBTTagCompound data = stack.getTagCompound();
        return (stack.getDisplayName() == null ? "" : stack.getDisplayName())
                + ' ' + (data == null ? "" : data.toString());
    }
}
