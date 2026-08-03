package com.shadowslice.autotntcart.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;

public class AutoTntCartReceiver {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(AutoTntCartPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            World world = player.getEntityWorld()
            context.server().execute(() -> handle(payload, world, player));
        });
    }

    private static void handle(AutoTntCartPayload payload, World world, ServerPlayerEntity player) {
        BlockPos targetPos = payload.pos();
        BlockPos placePos = targetPos.up();

        RegistryEntry<Enchantment> flameEntry = getEnchantmentEntry(world, Enchantments.FLAME);
        RegistryEntry<Enchantment> infinityEntry = getEnchantmentEntry(world, Enchantments.INFINITY);

        if (flameEntry == null) {
            player.sendMessage(Text.literal("无法获取火焰附魔数据"), true);
            return;
        }

        ItemStack flameBow = findFlameBowInHotbar(player, flameEntry);
        if (flameBow == null) {
            player.sendMessage(Text.literal("快捷栏中需要一把带有火矢附魔的弓"), true);
            return;
        }

        boolean hasInfinite = infinityEntry != null && EnchantmentHelper.getLevel(infinityEntry, flameBow) > 0;

        if (!hasRailAndMinecart(player)) {
            player.sendMessage(Text.literal("快捷栏中需要铁轨和TNT矿车"), true);
            return;
        }

        if (!hasArrowInHotbar(player)) {
            player.sendMessage(Text.literal("快捷栏中必须至少有一支箭"), true);
            return;
        }

        double distSq = player.squaredDistanceTo(
                placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5);
        if (distSq > 36.0) {
            player.sendMessage(Text.literal("目标位置太远"), true);
            return;
        }

        if (!world.getBlockState(placePos).isAir() && !world.getBlockState(placePos).canReplace(null)) {
            player.sendMessage(Text.literal("无法在此位置放置铁轨"), true);
            return;
        }

        if (!consumeItems(player, hasInfinite)) {
            player.sendMessage(Text.literal("消耗物品失败，请检查物品数量"), true);
            return;
        }

        shootFlameArrow(player, new Vec3d(
                placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5));

        world.setBlockState(placePos, Blocks.RAIL.getDefaultState(), 3);

        
        TntMinecartEntity cart = new TntMinecartEntity(EntityType.TNT_MINECART, world);
cart.setPosition(x, y, z);
world.spawnEntity(cart);
    }

    private static RegistryEntry<Enchantment> getEnchantmentEntry(World world, RegistryKey<Enchantment> key) {
        return world.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(key)
                .orElse(null);
    }

    private static ItemStack findFlameBowInHotbar(ServerPlayerEntity player, RegistryEntry<Enchantment> flameEntry) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(Items.BOW) && EnchantmentHelper.getLevel(flameEntry, stack) > 0) {
                return stack;
            }
        }
        return null;
    }

    private static boolean hasRailAndMinecart(ServerPlayerEntity player) {
        boolean hasRail = false;
        boolean hasCart = false;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(Items.RAIL)) {
                hasRail = true;
            } else if (stack.isOf(Items.TNT_MINECART)) {
                hasCart = true;
            }
        }
        return hasRail && hasCart;
    }

    private static boolean hasArrowInHotbar(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(Items.ARROW) || stack.isOf(Items.SPECTRAL_ARROW) || stack.isOf(Items.TIPPED_ARROW)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeItems(ServerPlayerEntity player, boolean hasInfinite) {
        PlayerInventory inv = player.getInventory();
        boolean railConsumed = false;
        boolean cartConsumed = false;
        boolean arrowConsumed = hasInfinite;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!railConsumed && stack.isOf(Items.RAIL)) {
                stack.decrement(1);
                railConsumed = true;
            } else if (!cartConsumed && stack.isOf(Items.TNT_MINECART)) {
                stack.decrement(1);
                cartConsumed = true;
            } else if (!hasInfinite && !arrowConsumed
                    && (stack.isOf(Items.ARROW) || stack.isOf(Items.SPECTRAL_ARROW) || stack.isOf(Items.TIPPED_ARROW))) {
                stack.decrement(1);
                arrowConsumed = true;
            }
            if (railConsumed && cartConsumed && arrowConsumed) {
                break;
            }
        }
        return railConsumed && cartConsumed && arrowConsumed;
    }

    private static void shootFlameArrow(ServerPlayerEntity player, Vec3d target) {
        World world = player.getEntityWorld();
        Vec3d from = player.getEyePos();
        Vec3d dir = target.subtract(from).normalize();

        ArrowEntity arrow = new ArrowEntity(EntityType.ARROW, world);
        arrow.setPosition(from.x, from.y, from.z);
        arrow.setVelocity(dir.x, dir.y, dir.z, 1.2F, 0.0F);
        arrow.setFireTicks(100);
        arrow.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
        arrow.setOwner(player);
        world.spawnEntity(arrow);
    }
}
