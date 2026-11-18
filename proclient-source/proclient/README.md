# Pro Client (Fabric, Minecraft 1.21.11)

Client-side Fabric mod: Entity ESP, Reach, Flight, HUD and a dark Click GUI.

## Build
Requirements: JDK 21, internet (first build downloads Minecraft, mappings and Fabric API).

    gradle wrapper --gradle-version 8.14.3     # once, needs Gradle installed; or open the folder in IntelliJ IDEA
    ./gradlew build                            # Windows: gradlew.bat build

Result: `build/libs/proclient-1.0.0.jar` (use this one, not the `-sources` jar).

Versions are in `gradle.properties` (MC 1.21.11, Yarn 1.21.11+build.4, Loader 0.18.4, Loom 1.14-SNAPSHOT,
Fabric API 0.141.3+1.21.11). If Gradle says a version does not exist, copy the current ones from https://fabricmc.net/develop.

## Install
1. Install Minecraft 1.21.11 and Fabric Loader 0.18.1+ (https://fabricmc.net/use).
2. Put `fabric-api` for 1.21.11 and `proclient-1.0.0.jar` in `.minecraft/mods`.
3. Start the game. Press **Right Shift** to open the Click GUI.

## Using it
- Left-click a module card: toggle. Right-click (or the +/- at the right): open its settings and keybind.
- Keybind row: click, press a key. Backspace/Delete clears, ESC cancels. Right-click the key button to clear.
- Type anywhere in the GUI to search modules. ESC or the GUI key closes it.
- Accent colour: ClickGUI module -> Accent Color.

## Config
`.minecraft/config/proclient.json`, auto-saved ~1.5 s after a change, on GUI close and on exit.
Written atomically (temp file + move), previous good copy kept as `.bak`; an unreadable file is renamed
`proclient.json.corrupt-<time>` and defaults/backup are used. Default file: `config/proclient.default.json`.

## Features
- Entity ESP: glow outline (through walls) per category (Players, Mobs, Animals, Items), per-category toggle and colour,
  range, name / distance / health tags (first person).
- Reach: 3-8 blocks via vanilla interaction-range attributes; single-player only.
- Flight: creative-style flight with 0.5x-10x speed; single-player only.
- Both are switched off automatically when you join a multiplayer server, and cannot be enabled there.
- HUD: FPS, coordinates, precise XYZ, biome, entity count, active module list, corner + offset, scale.
- Click GUI: 5 categories, rounded panels, animations, search, keybind editor, sliders/toggles/mode buttons, accent colour.

## Known limits
- ESP tags are drawn only in first person; their position uses the FOV option, so they drift slightly while sprinting.
- ESP works on multiplayer servers. Many servers forbid it; you are responsible for where you use it.
- Outline thickness is vanilla's and cannot be changed.
