package com.shadowslice.autotntcart;

import com.shadowslice.autotntcart.network.AutoTntCartPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.text.Text;

public class AutoTntCartClient implements ClientModInitializer {

    private static KeyBinding keyBinding;
    private static boolean lastPressed = false;

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autotntcart.trigger",
                InputUtil.Type.KEYSYM,
                67, // GLFW_KEY_C
                "category.autotntcart"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
    }

    private void onEndTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        boolean pressed = keyBinding.isPressed();
        if (pressed && !lastPressed) {
            onKeyPress(client);
        }
        lastPressed = pressed;
    }

    private void onKeyPress(MinecraftClient client) {
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            sendMessage(client, "请瞄准一个方块的顶面");
            return;
        }

        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        if (hit.getSide() != Direction.UP) {
            sendMessage(client, "请瞄准方块的顶面（铁轨只能放在顶部）");
            return;
        }

        RegistryEntry<Enchantment> flameEntry = getEnchantmentEntry(client, Enchantments.FLAME);
        RegistryEntry<Enchantment> infinityEntry = getEnchantmentEntry(client, Enchantments.INFINITY);

        if (flameEntry == null) {
            sendMessage(client, "无法获取火焰附魔数据");
            return;
        }

        String missing = getMissingItemsMessage(client, flameEntry, infinityEntry);
        if (missing != null) {
            sendMessage(client, missing);
            return;
        }

        ClientPlayNetworking.send(new AutoTntCartPayload(hit.getBlockPos(), hit.getSide()));
    }

    private RegistryEntry<Enchantment> getEnchantmentEntry(MinecraftClient client, RegistryKey<Enchantment> key) {
        if (client.world == null) {
            return null;
        }
        try {
            return client.world.getRegistryManager().getEntryOrThrow(key);
        } catch (Exception e) {
            return null;
        }
    }

    private String getMissingItemsMessage(MinecraftClient client, RegistryEntry<Enchantment> flameEntry,
                                           RegistryEntry<Enchantment> infinityEntry) {
        // NOTE: decompiled behavior scans the full "main" inventory list (36 slots,
        // hotbar + storage), not just the 9 hotbar slots. This differs from the
        // server-side check below, which is strictly hotbar-only. Preserved as-is
        // from the original bytecode; tighten to hotbar-only (0-8) if that was a bug.
        var mainInventory = client.player.getInventory().main;
        boolean hasRail = false;
        boolean hasCart = false;
        ItemStack flameBow = null;
        boolean hasArrow = false;

        for (int i = 0; i < mainInventory.size(); i++) {
            ItemStack stack = mainInventory.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.isOf(Items.RAIL)) {
                hasRail = true;
            } else if (stack.isOf(Items.TNT_MINECART)) {
                hasCart = true;
            } else if (stack.isOf(Items.BOW)) {
                if (EnchantmentHelper.getLevel(flameEntry, stack) > 0) {
                    flameBow = stack;
                }
            } else if (stack.isOf(Items.ARROW) || stack.isOf(Items.SPECTRAL_ARROW) || stack.isOf(Items.TIPPED_ARROW)) {
                hasArrow = true;
            }
        }

        if (!hasRail) {
            return "快捷栏中缺少铁轨";
        }
        if (!hasCart) {
            return "快捷栏中缺少TNT矿车";
        }
        if (flameBow == null) {
            return "快捷栏中需要一把带有火矢附魔的弓";
        }

        boolean hasInfinity = infinityEntry != null && EnchantmentHelper.getLevel(infinityEntry, flameBow) > 0;

        if (!hasInfinity && !hasArrow) {
            return "快捷栏中需要至少一支箭（无限附魔的弓除外）";
        }
        if (hasInfinity && !hasArrow) {
            return "即使使用无限弓，快捷栏中也必须有一支箭（不会被消耗）";
        }
        return null;
    }

    private void sendMessage(MinecraftClient client, String msg) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(msg), true);
        }
    }
}
