package com.alienwc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AlienWindChargeClient implements ClientModInitializer {

    public static final String MOD_ID = "alien_wc";
    public static KeyBinding alienWcKeybind;

    @Override
    public void onInitializeClient() {
        // Register keybind (Default: 'V', Category: "Combat Tech")
        alienWcKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.alien_wc.launch",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.alien_wc.combat_tech"
        ));

        // Register client tick listener
        ClientTickEvents.END_CLIENT_TICK.register(AlienWcHandler::onClientTick);
    }
}
