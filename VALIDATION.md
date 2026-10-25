# ball's hud studio 1.7 validation

Version 1.7 (2026-10-25): corrected reversed freecam strafing. Minecraft's level extractor explicitly skips LocalPlayer when a different entity owns the camera; the freecam rendering hook now bypasses that specific guard when Show player is enabled. Camera entity previous transforms are updated each tick, and detached-camera culling supports passing through blocks. The production build passed and releases/hud-studio-1.7.0.jar contains version 1.7.0 metadata and no test classes. The client again encountered the known native 0xC0000005 shutdown crash after completing its assertions.

Added freecam Controls, Movement and Display settings: hold/toggle activation, exit on damage, horizontal/crosshair flight, independent horizontal/vertical speeds, sprint multiplier, block collision, player visibility, HUD visibility and submersion fog. Hold activation requires a fresh release after damage exits freecam. Zoom adds Show HUD and Cinematic zoom, with Z as its fresh default binding. Saved key overrides remain intact. Menus now use Game HUD, Inventory, Keystrokes, Stats, Camera and Visuals; all Studio screens pause single-player through the vanilla pause-screen mechanism.

The isolated 1.7 client wrote PASS at 15:36:32. Assertions covered category order, saved freecam/zoom toggles, default Z binding, actual single-player pause/resume, correct A/D movement, player body extraction with Show player ON/OFF, hold activation/release, damage exit, unchanged player aim/position, zoom HUD/cinematic activation and reset, scroll bounds and unchanged hotbar selection. Inspected screenshots of the freecam settings, visible player body and hidden HUD. Existing HUD, inventory and combo regressions completed. Independent speed values, collision and submersion toggles are implemented but have not each been exercised in a live-world assertion. Multiplayer and optional reference-mod features such as tripods, fullbright and player interaction remain outside this revision.

Freecam/zoom revision (2026-10-25): inspected the supplied Freecam 1.5.1 and WI Zoom 1.7 JAR bytecode for their default movement and zoom behavior. Implemented level freecam flight, independent vertical movement, normalized horizontal diagonals, a latched 1.5x forward sprint boost, a separate render camera entity, damage-triggered exit and hidden submersion fog. Zoom now scales both world and HUD FOV, adjusts sensitivity before vanilla's cubic curve, scrolls by +10% / -10% between 1x and 50x, consumes the scroll event and resets on release. Fresh defaults are 3x zoom and 1 block/tick freecam speed; existing saved settings remain valid. This matches the core controls and default camera behavior; optional reference-mod settings and tripod slots are not reproduced.

The isolated client wrote its PASS marker at 15:18:02. New assertions verified horizontal flight while looking down, detached camera ownership, unchanged player position/aim, camera restoration, exit after actual server damage, real mouse-scroll magnification, unchanged hotbar selection, zoom bounds and reset on the next hold. All existing HUD and combo regression assertions also completed. The process then exited with the previously documented native status 0xC0000005 during shutdown. Multiplayer compatibility and optional reference-mod behaviors have not been tested.

Tested in the actual Minecraft 26.3 client with Fabric API 0.163.0+26.3 and Mod Menu 21.0.0.

Tests: `tests/client/java/dev/hudstudio/client/HudSmokeTest.java`. Run with `gradlew.bat runClient --no-configuration-cache -I client-test.gradle`. Test worlds, screenshots, logs and the PASS marker are generated under `build/client-test`; no extracted Minecraft sources or local Maven repository are required.

Verified ordinary `gradlew.bat runClient` startup with configuration caching enabled. Resource expansion and JAR license naming capture version/name values during configuration rather than accessing Gradle's project from task actions. `gradlew.bat clean build` passed both when storing and reusing its configuration cache.

- Checked fresh defaults: non-vanilla modules disabled, no custom slots, native offhand rendering preserved.
- Verified Keystrokes, Stats, Vanilla HUD and Custom Slots category selection.
- Typed into the live search field and verified filtering, empty results, retained focus and retained cursor position.
- Confirmed another module's body and resize corners cannot be selected or manipulated while the arranger is locked to its opening module.
- Verified selected-module dragging, proportional corner resizing and Reset, retaining another module's state.
- Checked both CPS strings omit left/right labels. All stats and single-row keystroke modules measure 64 × 20 pixels, matching WASD's 20-pixel key height and 64-pixel cluster width.
- Exercised colour presets, hue selection, saturation/brightness dragging, the opacity slider, saved appearance settings and appearance reset.
- Created and repositioned a helmet display from the inventory picker, checked 22 × 22 complete hotbar frame bounds and saved assignment.
- Verified Shift bypasses movement snapping, Shift resizing snaps to 5% steps, and releasing Shift during a drag restores continuous resizing.
- Applied the individual slot opacity slider, verified draft isolation before confirmation and persisted opacity after confirmation.
- Compared screenshot border pixels across four custom displays before and after changing the global colour and opacity; their appearance was unchanged. Compared live HUD and arranger borders to verify the editor does not obscure the slot bottoms.
- Inspected screenshots of the inventory picker (including corrected Crafting text), fractional-scale selection outline, four normal hotbar slots, category menu, appearance controls, vanilla defaults and standardized stat/key sizes.
- Checked WASD/stat row alignment in both movement directions at 100%, 137% and 200% scale, and exercised row snapping through the arranger mouse handlers.
- Verified same-scale custom slots join at a 20-pixel stride at 100%, 137% and 200% scale. Checked neighbours and pixel-tested black end borders on standalone, four-slot and split hotbars.
- Verified native offhand rendering remains active after changing global appearance. Compared opaque frame pixels of a moved offhand display before and after changing the theme and setting global opacity to zero; transparent corners show the changing world and are excluded.
- The test recorded a PASS marker after these assertions.

Additional 1.6 checks cover CPS in Keystrokes, the five independent Stats modules, Appearance above the categories, and the Client Tweaks category/settings. Verified keyboard rebinding and unchanged view matrices with walking bob and hurt tilt suppressed. Tested vertical slot neighbours and snapping, and inspected the complete vertical hotbar frame.

Exercised freecam movement while asserting the player's horizontal position and aim remain unchanged. Verified freelook starts while held, leaves player aim unchanged and restores first person on release. Checked that zoom narrows the actual camera FOV and that explosion particles are suppressed.

Spawned an entity on the integrated test server and landed two real melee attacks; server-confirmed damage started Good with the expected hit count. Applied server-side damage to the player and verified interruption. Deterministic scoring checks cover the 1.5x critical multiplier, all eight ranks, shorter deadlines at each rank, timeout and interruption. All eight bundled Ogg resources were present. Inspected screenshots of Good, Legendary with critical feedback, Combo Broken, vertical slots, freelook, zoom and the revised menu. Remote servers with nonstandard damage-event behaviour have not been tested.

The automated client still encounters the previously observed native access violation during shutdown, after its assertions complete. Earlier native reports identify AMD's `atio6axx.dll`; a clean runtime exit remains unverified.

Native positioning outlines are approximate for variable-size content. Multiplayer and compatibility with other HUD replacements have not been tested. Production builds exclude test classes and test-mod entrypoints.

Combo revision (2026-10-25): client assertions passed with a diamond sword, verifying confirmed hits report measured health loss above the old fist-damage values. Checked second-hit activation, halved tier timers, accumulated measured critical damage without a second multiplier, capped tiers and end-summary totals. Legendary playback now restarts on each further confirmed measurable hit. The client again exited with native status 0xC0000005 after writing its PASS marker; clean shutdown remains unverified.

Projectile/weapon feedback revision: client assertions passed for server damage commands attributed to the local player through arrow, spectral-arrow, firework-rocket, trident and snowball source entities, with exact measured totals and HIT feedback. Equivalent ender-pearl events were excluded. Real held-use spear input plus server spear damage events verified Speared feedback; a marked left-click piercing swing verified Damage feedback, and mace-smash events verified CRUSHED. These checks exercise server damage attribution and health synchronization; bow/crossbow firing trajectories and natural projectile collisions were not individually automated. The final run wrote its PASS marker at 14:52:36 before the known native shutdown crash.
