# Alien Wind Charge Fabric Mod (MC 1.21+)

A client-side Fabric mod that executes a frame-perfect Alien Wind Charge launch at the press of a keybind.

## Features
- **Keybind Trigger**: Default key is `V` (configurable in Controls -> Combat Tech).
- **Auto-Swap**: Detects Wind Charge in hotbar automatically.
- **Pitch Snap**: Snaps pitch down to 90 degrees and back in 2 ticks to prevent anti-cheat desync.
- **Restoration**: Automatically returns to original active hotbar slot and camera direction.

## How to Build
1. Make sure Java 21 JDK is installed.
2. Open terminal in this folder and run:
   - Linux/macOS: `./gradlew build`
   - Windows: `gradlew.bat build`
3. Built `.jar` file will be generated in `build/libs/`.
4. Place the `.jar` into your Minecraft `.minecraft/mods` folder along with Fabric API.
