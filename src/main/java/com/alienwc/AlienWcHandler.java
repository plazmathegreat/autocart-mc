package com.alienwc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

public class AlienWcHandler {

    // Sequence States
    private enum State {
        IDLE,
        INITIATING, // Tick 1: Rotate + Swap
        EXECUTING   // Tick 2: Use Item + Restore
    }

    private static State currentState = State.IDLE;
    private static long lastExecutionTime = 0;
    private static final long COOLDOWN_MS = 250; // 250ms anti-spam cooldown

    // Stored player state
    private static float originalPitch = 0f;
    private static float originalYaw = 0f;
    private static int originalSlot = -1;
    private static int windChargeSlot = -1;

    public static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            resetState();
            return;
        }

        long currentTime = System.currentTimeMillis();

        // 1. Check for keybind press in IDLE state
        if (currentState == State.IDLE) {
            if (AlienWindChargeClient.alienWcKeybind.wasPressed()) {
                if (currentTime - lastExecutionTime < COOLDOWN_MS) {
                    return; // Ignore if on cooldown
                }

                // Locate Wind Charge in hotbar (slots 0 - 8)
                windChargeSlot = findWindChargeSlot(client);
                if (windChargeSlot == -1) {
                    // Wind Charge not found in hotbar
                    return;
                }

                // Step A: Save state
                originalPitch = client.player.getPitch();
                originalYaw = client.player.getYaw();
                originalSlot = client.player.getInventory().selectedSlot;

                // Move to Tick 1
                currentState = State.INITIATING;
            }
            return;
        }

        // 2. TICK 1: Swap Slot & Snap Rotation
        if (currentState == State.INITIATING) {
            // Select Wind Charge slot
            client.player.getInventory().selectedSlot = windChargeSlot;

            // Set client pitch down (90.0 degrees)
            client.player.setPitch(90.0f);

            // Sync rotation packet to server ahead of item use
            client.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.LookAndOnGround(
                    client.player.getYaw(),
                    90.0f,
                    client.player.isOnGround()
                )
            );

            // Advance to Tick 2
            currentState = State.EXECUTING;
            return;
        }

        // 3. TICK 2: Use Wind Charge & Restore Original State
        if (currentState == State.EXECUTING) {
            // Right-click / Use item
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);

            // Restore original hotbar slot
            if (originalSlot != -1) {
                client.player.getInventory().selectedSlot = originalSlot;
            }

            // Restore original camera angles
            client.player.setPitch(originalPitch);
            client.player.setYaw(originalYaw);

            // Sync original rotation packet back to server
            client.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.LookAndOnGround(
                    originalYaw,
                    originalPitch,
                    client.player.isOnGround()
                )
            );

            // Update cooldown and return to IDLE
            lastExecutionTime = System.currentTimeMillis();
            resetState();
        }
    }

    private static int findWindChargeSlot(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) {
                return i;
            }
        }
        return -1;
    }

    private static void resetState() {
        currentState = State.IDLE;
        originalSlot = -1;
        windChargeSlot = -1;
    }
}
