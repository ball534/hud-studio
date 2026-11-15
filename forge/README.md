# HUD Studio 1.8 for Forge 1.8.9

This project restores the Forge implementation from the supplied `releases/hud-studio-1.7.0-forge1.8.9.jar`, with decompiled source mapped back to MCP stable 22 names. Its build uses the supplied Forge1.8.9Template layout with Essential Loom 1.6.21. The original Forge release is retained. The single recovered mixin comparison requiring a Java source correction casts `this` to Object before comparing it to the player.

Run Gradle with Java 21 and install a Java 8 JDK for compiling and running Minecraft. If Java 8 is not automatically detected, set `JAVA8_HOME` to its JDK directory. Use this directory's Gradle 8.8 wrapper:

```powershell
.\gradlew.bat clean build
```

The installable artifact is `build/libs/hud-studio-1.8.0-forge1.8.9.jar`. Copy it into a Forge 11.15.1.2318 client's mods folder. Open Studio with Right Shift. Settings use `config/hud-studio-forge.json`, preserving the previous Forge configuration.

Full Bright appears under Visuals and defaults to OFF. Its mixin supplies gamma 100 to `EntityRenderer.updateLightmap` without changing `GameSettings.gammaSetting` or options.txt. Disable it to return to the current brightness setting. Effects offers a three-dot/right-click menu with Time remaining, default OFF. Timers follow each icon's position and scale. Forge 1.8.9 lacks native HUD effect icons, so this project retains the custom effect renderer from 1.7.

For the isolated live-client checks:

```powershell
.\gradlew.bat runClient -I client-test.gradle
```

The test creates a flat world under `build/client-test`, verifies actual lightmap saturation and restoration, checks saved gamma and UI setting persistence, and captures effect timers ON/OFF and scaled screenshots. It writes `build/client-test/hud-result.txt`. Ordinary builds exclude the test mod; run a clean production build after integration testing.

Shared usage documentation is in [the root README](../README.md).
