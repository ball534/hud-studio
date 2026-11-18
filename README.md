# ball's hud studio - Fabric 26.3 and Forge 1.8.9

A client-side HUD mod with compact modules, a category sidebar, waypoints, a world map and global appearance controls.

## Installation and defaults

Replace the previous JAR with the appropriate release in your client's `mods` folder:

- **Fabric 26.3:** `hud-studio-1.9.jar`. Requires Fabric API for Minecraft 26.3, Mod Menu 21.0.0+, Fabric Loader 0.19.5+ and Java 25. Open **Mods > ball's hud studio > Configure**.
- **Forge 1.8.9:** `hud-studio-1.9-forge1.8.9.jar`. Requires Forge 11.15.1.2318 and Java 8. Open with **Right Shift**, rebindable in Controls. Minecraft 1.8.9 has no offhand, so that module is exclusive to Fabric.

Servers do not need ball's hud studio. Install only the JAR matching your loader and Minecraft version.

**Fresh installations keep the vanilla HUD.** All keystrokes, stats and client tweaks start disabled, there are no custom slots, and Minecraft renders the original offhand display until it is customized. Existing saved module settings and inventory assignments are preserved when upgrading.

On Forge, enable the Effects module to add status icons to the HUD; vanilla 1.8.9 has no HUD status icons.

## Module menu

The module menu and its settings pause single-player, like the Escape menu. Multiplayer keeps running, following vanilla pause behavior.

Choose a category from the left sidebar:

- **HUD:** scoreboard, boss bars, effects, player list, Combo Meter, all keystrokes and stats.
- **Inventory:** Add slot, offhand and custom inventory displays.
- **Camera:** zoom, freelook and freecam.
- **Visuals:** Full Bright, camera shake and explosion particle suppression.
- **Waypoints:** the waypoint manager, with New Waypoint and compact Enabled, Show Names and Show Distances checkboxes.

All categories share the Waypoints menu frame, sidebar and Back-button position. HUD, Inventory, Camera and Visuals use the same responsive card size across sections and fill that fixed content grid, fitting their modules without a search bar or scrolling. Cards show a title and ON/OFF switch, with descriptions in tooltips and icons when space allows. Waypoints keeps the flat Studio-style search field, management rows and compact checkboxes. Back sits beneath Appearance at the same width and height; waypoint footer actions share its baseline. Menu text uses the native font on whole GUI pixels.

Click a card's body to open its arranger. Click its switch to toggle visibility. Space toggles a focused card; Enter opens it. Scroll the grid when more cards are available.

## Locked HUD arranger

Only the module used to open the arranger can be moved, resized or reset. Other HUD pieces provide visual context.

- Drag the selected piece to move it. A 2-pixel grid matches the key spacing, and nearby modules snap to module edges and both WASD rows, including at custom scales. Hold Shift to bypass all snapping for fine positioning.
- Drag either bottom corner to resize it proportionally; the cursor changes to a diagonal resize cursor. Hold Shift to snap its scale in 5% steps.
- Arrow keys nudge it by one pixel.
- **Reset** restores that module's default position and size, retaining its visibility and inventory assignment.
- **Back** or Escape saves and returns to the module menu.

The arranger has only Back and Reset buttons, with no top or bottom bars. A disabled module can be arranged through its card without enabling it.

Inactive native modules use approximate outlines and representative preview content. Their normal activation rules still apply: a scoreboard needs an objective, the player list needs its key held, and effects need active status effects.

## Effect timers

In **HUD**, right-click the **Effects** card or click its three-dot menu, then enable **Time remaining**. Each effect icon displays its remaining duration; infinite effects show **∞**. The timer moves and scales with its icon and also appears in the arranger preview. Disable the option to show icons alone. Timers start disabled and the setting is saved across restarts.

## Global appearance

Choose **Appearance** above the category menu. Select a hue from the colour strip, then choose saturation and brightness in the colour square, or use a preset swatch.

The opacity slider changes keystroke and stat panels. Custom inventory displays have independent opacity and retain their vanilla colours. Offhand also retains its vanilla colours and opacity, including when moved or resized. Labels and item icons stay visible. The preview updates immediately. Colours also appear in menu accents and active toggles. **Reset appearance** restores the default black theme and opacity.

The original offhand display remains native while its module uses its defaults, regardless of global appearance settings. Customizing its position, scale or empty-slot setting uses the complete movable frame.

## Inventory displays

In Inventory, click **Add slot**. The picker mirrors the vanilla inventory, including armour, offhand, the 27 main inventory slots and all nine hotbar slots. Select a slot and choose **Add display** to open its arranger.

Standalone displays use a complete unselected vanilla hotbar frame, measuring 22 Ã— 22 pixels, with standard item counts and durability overlays. Each display has its own **Slot opacity** slider in the inventory picker, independent of the global theme and opacity. Confirm the picker to save its assignment and opacity. They follow the item occupying the assigned slot without moving inventory items.

Drag equal-scale custom slots beside or above one another to snap them into a horizontal or vertical mini hotbar. Adjacent displays share their dividers and retain black borders at the two ends. A row of N slots measures (20 Ã— N + 2) Ã— 22 pixels at 100% scale; a column measures 22 Ã— (20 Ã— N + 2). Create another slot and snap it beside either end to extend the row. Moving, hiding or removing a neighbour restores the exposed end border. Hold Shift while positioning to bypass joining, as with other snapping.

Right-click a custom slot card or use its three-dot menu to reassign it, hide it when empty or remove it. The offhand card also offers hide-empty settings. The title-screen picker uses an empty inventory preview; join a world to see your items.

## Input and stat displays

WASD follows movement bindings. SPACE, SNEAK and SPRINT follow their corresponding actions, and LMB / RMB reflects actual mouse buttons.

Standard stats and single-row keystroke bars are 64 Ã— 20 pixels at default scale. Combo Meter uses a 140 Ã— 60 canvas for its rank, score, hit feedback and timer. WASD uses 20 Ã— 20 keys with 2-pixel gaps, making a 64 Ã— 42 cluster. Default placement uses consistent 2-pixel gaps between rows. The two CPS modules both display only the click count and **CPS**; their menu titles distinguish left and right. Speed is horizontal movement, the clock uses local time, and memory is Java heap usage.

## Camera and Visuals

These are client behaviour settings, so clicking their cards opens settings rather than the HUD arranger. All start disabled. Enable them in Camera or Visuals before using their keys. Bind keys in each tweak screen or in Minecraft Controls; bindings are saved in Minecraft options.

| Tweak | Default control | Behaviour |
|---|---|---|
| Zoom | Hold Z | Starts at 3x; scroll while held to zoom between 1x and 50x. Release resets magnification. Starting zoom is configurable; sensitivity follows WI Zoom's curve. |
| No camera shake | Toggle in Studio | Removes walking bob and hit tilt without changing vanilla settings. |
| Full Bright | Toggle in Studio | Uses full ambient lighting on Fabric and maximum lightmap gamma on Forge to brighten dark areas. Disabling it restores normal lighting; your saved brightness setting stays intact. |
| Freecam | Press F6 | Toggles a detached camera; WASD flies horizontally, jump/sneak ascends/descends. Sprint while moving forward gives a 1.5x horizontal boost until forward movement stops. Speed is adjustable. |
| No explosions | Toggle in Studio | Hides explosion bursts, emitters and explosion block debris while retaining sounds and gameplay. |
| Freelook | Hold Left Alt | Orbits in third person while the character keeps its aim and movement direction; releasing restores the previous view. |

Freecam explores already loaded chunks with a separate camera entity and correct screen-relative strafing. Its Controls tab offers hold/toggle activation and exit on damage. Movement offers horizontal or crosshair-directed flight, independent horizontal/vertical speeds, sprint multiplier and block collision. Display offers player visibility, HUD visibility and submersion fog. By default your player body is visible, the held hand is hidden, and the camera passes through blocks. On Fabric you can open your inventory and select slots in your real hotbar while in freecam. Freecam clears block/entity targeting and blocks attacking, breaking, item use, dropping, hand swapping and inventory slot actions; it never teleports the player or changes their flight abilities. Taking damage, leaving the world, dying or disabling the tweak restores the previous camera entity and mode. Normal player physics continue while detached. Freelook and freecam do not change the character's yaw or pitch. Zoom settings include starting magnification, Show HUD and Cinematic zoom (smooth mouse movement while held). These options do not change your F1 HUD setting or saved cinematic camera setting. Existing saved speed, starting zoom and key bindings are retained; Z is the default for fresh key bindings.

## Waypoints and world map (Fabric 1.11)

| Key | Action |
|---|---|
| **N** | Create a waypoint draft at rounded player XYZ. |
| **U** | Open the waypoint manager for the current dimension. |
| **M** | Open the world map. M, Escape or its red X closes it. |

Rebind under **Controls > Key Binds > ball's hud studio: Navigation**. Simple Voice Chat also binds N/M; resolve those conflicts in Controls. Studio's **Waypoints** category and the U key open the same management page. There are no separate Manage or World map buttons in Studio.

The editor is a 176 by 166 GUI-pixel panel, matching the vanilla inventory footprint. New markers default to **Waypoint** and a random vanilla text colour. Name, colour and whole-number XYZ are editable. Save commits; Cancel/Escape discards. The manager defaults to the current dimension, with an optional All dimensions filter. Compact rows show a coloured diamond, name, coordinates, then inset Edit, Hide/Show and Delete buttons. Delete removes and saves immediately, without confirmation. Mouse-wheel and scrollbar scrolling replace pagination. No world title, dimension tags, page counts or map button appear in the manager.

Three compact checkboxes beside **New Waypoint** save immediately: **Enabled**, **Show Names** and **Show Distances**. These checkboxes affect only waypoints in the world; the map always shows its visible markers, names and distances. The previous minimum/maximum text and block sliders are removed, and their old saved limits no longer apply. Per-waypoint Hide also suppresses the corresponding map marker. The manager lists shown waypoints first and hidden waypoints last, sorting each group by name without case sensitivity (digits before letters). Hidden names are italic, and a separate coordinate column uses X: xxx, Y: yyy, Z: zzz. World labels use client-only vanilla text displays with camera-facing billboards, the vanilla translucent background and normal perspective sizing. The highlight covers the block below the saved waypoint Y; its label sits just above that block and uses the saved waypoint colour exactly.

The fullscreen map is a centred 2:1 parchment rectangle, scaled to fit with at least 16 GUI pixels of surrounding space. It uses the actual vanilla filled-map background texture. Corners, edges and the centre tile at native pixel scale; partial tiles are cropped so the parchment grain and ragged edges are never stretched. Unknown terrain shows a seamless pale fog tile with subtle grey squiggles and a rounded, softly blended dark outline against explored terrain, and the area outside the paper stays transparent. The close button is a red lowercase x centred in a small dark box at the paper's upper-right corner; hovering turns the box red at the same 50% opacity and the x black. Map drawing and mouse input stay inside the parchment edges. The map fades in and slides up when opened. Drag to pan and scroll to zoom around the cursor; sampling stays on a world-anchored pixel grid so fractional pans do not reshape the terrain pixels. N and U also work on the map. It uses vanilla filled-map colours and scale-zero height/water-depth shade rules, without biome tint replacement. The player marker is the vanilla `textures/map/decorations/player.png` sprite. Names and distances always appear on marker hover, regardless of the world toggles, fading and sliding up above the marker.

Waypoints use Minecraft's vanilla map banner textures, choosing the closest dye colour to the saved waypoint colour. Left-clicking a waypoint does nothing. Right-clicking a location offers New Waypoint, Copy Coords, Teleport and Close; right-clicking a pin offers Edit, Copy Coords, Teleport, Delete and Close. Location coordinates use the recorded surface height plus one, falling back to the player's Y for unsurveyed terrain. Copy Coords copies `X Y Z` and briefly shows a green Coords Copied! notice with X/Y/Z underneath inside the map. Teleport sends the normal `/tp X Y Z` command and requires cheats or server permission. Delete saves immediately. Escape or clicking outside dismisses the popup. All waypoint footer buttons stay on one baseline; very narrow windows show checkbox tooltips instead of labels.

The world map always surveys and displays the surface of the current dimension, including while the player is underground. All surveying uses already received client chunks.

Missing coverage takes priority over refreshes; unsuccessful lookups are retried and unsurveyed terrain is captured before unload. Up to four chunks are processed per tick with an approximate 3 ms sampling budget. Compressed snapshots save on a background worker, with up to 256 regions in memory. Legacy surface tiles remain saved and are recoloured when their chunks load. Previously unreceived terrain still needs to be loaded to survey it.

Waypoints and map tiles live under `config/hud-studio-worlds/<world-or-server-hash>/`, separated by world/server, dimension and map layer. Back up that directory to preserve markers and exploration. HUD settings remain separate. Xaero is not required, and its save files are not imported.

## Combo Meter

Enable **HUD > Combo Meter** and arrange it like other HUD pieces. Its default position has 18 pixels of right padding and 28 pixels of top padding. Saved custom placements are retained; use Reset in the arranger to apply the new default.

The first confirmed damaging hit primes the streak; the meter appears at Good on the second hit, including both hits and their damage in its totals. Each subsequent hit advances one tier through Great, Brutal, Savage, Stylish, Sensational, Unstoppable and Legendary. Further hits continue adding damage and hits at Legendary. Misses do not advance the meter. Taking damage or inactivity ends the combo, showing total damage and hit count for four seconds.

The meter accumulates observed target health and absorption loss after server-confirmed melee and projectile hits attributed to the local player. Feedback reads `Damage +x` or `Crit +x` for melee, `HIT +x` for damaging projectiles, `Speared +x` for held spear attacks, and `CRUSHED +x` for mace attacks. Left-click spear attacks use `Damage +x`. Critical damage is measured, not multiplied again. Bow and crossbow arrows, crossbow fireworks, tridents and other damaging player projectiles count; ender pearls and hits that remove no health or absorption do not count. Snowballs count when they actually damage their target. Damage is limited to the health actually removed, including on killing blows. The client waits briefly for health metadata after the damage event. Servers that hide target health changes cannot supply reliable damage totals; unmeasurable hits are not assigned guessed damage.

Each new tier plays its bundled combo clip, and every further confirmed hit at Legendary restarts the Legendary clip. The bottom timer lasts four seconds at Good, decreasing by 0.3 seconds per tier to 1.9 seconds at Legendary. Rank colours, pulsing text and hit feedback remain; the left accent and top tier-change bar have been removed. Pausing single-player pauses the timer.

## Settings and development

Settings are saved in `config/hud-studio.json` on Fabric and `config/hud-studio-forge.json` on Forge. Existing permitted placements and inventory assignments are preserved. Older combined input/stat panels are migrated to independent modules.

The repository root contains the Fabric source. Run `gradlew.bat runClient` for the development client, or `gradlew.bat build` to produce `build/libs/hud-studio-1.9.jar`. The independent Forge project is in [`forge`](forge/README.md); run its wrapper from that directory to produce `forge/build/libs/hud-studio-1.9-forge1.8.9.jar`. Waypoints and the world map are currently Fabric features.

Reusable client integration tests live in `tests/client`. Run `gradlew.bat runClient --no-configuration-cache -I client-test.gradle`; they use an isolated runtime in `build/client-test`, preserving your normal development world and settings. A successful assertion run writes `build/client-test/hud-result.txt`. See VALIDATION.md for the known native-driver shutdown issue.

To test compatibility, copy third-party mod JARs into `build/client-test/mods` (omit HUD Studio itself) and add `-PcompatibilityClient` to the test command. This packages the test mods and uses a normal Fabric launch, avoiding Essential's development-mode remapping error on unobfuscated Minecraft 26.3. The suite checks actual dark-room screenshots with Full Bright OFF, ON and restored, plus detached-camera player render extraction.

The project keeps source and resources in `src`, Gradle's wrapper in `gradle`, and the current installable JAR in `releases`. `build`, `.gradle` and test runtime files are generated and can be recreated. Development worlds, settings and screenshots are kept in `run`.

The implementation uses Fabric's HUD API and Mod Menu's configuration API. Other mods replacing the same HUD methods may require compatibility adjustments.

License: CC0-1.0, inherited from the supplied template.
