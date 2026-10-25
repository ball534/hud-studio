# ball's hud studio 1.6 validation

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
