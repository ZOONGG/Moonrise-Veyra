# Veyra

Veyra is an independently evolving Weave client for Minecraft 1.8.9. The
project starts from the archived [Tryflle/stormy](https://github.com/Tryflle/stormy)
codebase and is being separated into its own identity, configuration, design
system, and feature roadmap.

Veyra is under active development. The ClickGUI key is `Enter`.
[Русский README](README.ru.md).

## Features

- Combat, movement, player and render modules inherited from Stormy.
- Redesigned ClickGUI with search (including Russian keyboard-layout matching),
  favorites, themes, module descriptions and configurable binds.
- Additional player tools, including AutoTool, AutoFish, SpeedMine and VoidClutch.
- Separate Veyra configuration and module-toggle notifications.

## What's Different from Stormy?

Comparison is against the archived Stormy `main`, not a promise that inherited
modules behave identically or work on every server.

| Area | Stormy | Veyra |
| --- | --- | --- |
| Retained baseline | AutoClicker, AimAssist, Reach, Velocity, Backtrack, movement modules, ESP, inventory tools | These module families remain registered; `Enter` still opens ClickGUI. |
| Interface and configuration | Stormy GUI and identity | Redesigned GUI, search, favorites, themes, descriptions, bind modes and separate `.weave/veyra` configuration. |
| Player tools | No SpeedMine or Pearl → Fireball macro in the module registry | SpeedMine scales local block-breaking progress from 1.00 to 2.50 without changing the game clock. VoidClutch provides the bind-only Pearl → Fireball sequence described below. |
| Timer | Speed 0.50–2.50, updated on ticks; reset in ClickGUI | Speed 0.00–2.50, updated every frame; 0.00 pauses client ticks while GUI and control binds remain serviced. Strafe-only gating and clock ownership/restore logic were reworked. `TimerSpeedPolicy` is internal code, not a separate module. |
| Other additions | Original module set | JumpReset, TargetFilter, NoJumpDelay, AntiAFK, AutoTool, AutoBlockIn, AutoFish, NoUseDelay, BedESP, BedPlates, MurderFinder and Freecam are registered. |
| Changed availability | AutoPlace registered | AutoPlace is no longer registered. SafeWalk has bridge-assist logic and AutoBlockIn is available; these are not a claim of identical AutoPlace behavior. Legacy Clutch is also unregistered. |

**Player: Pearl → Fireball.** Assign a press bind to **VoidClutch** and keep an
Ender Pearl and a fire charge in the hotbar. It aims fully upward over about
50 ms, selects the pearl, waits at least 50 ms and invokes vanilla right-click,
then selects and uses the fire charge with 30 ms minimum waits. Actions run on
client ticks, so actual delays can be longer; at Timer speed 0.00 the sequence
cannot progress. It cancels on loss of focus, open GUI, death or world changes;
smooth camera prevents starting. A fire charge launches a fireball only where
the game/server implements that behavior. The separate `PearlFireball` and
`FireballPractice` classes remain in source but are **not registered or available
in ClickGUI**; practice mixins are not enabled.

Timer also exposes **Experimental freeze**, disabled by default, which attempts
to stop local movement while retaining normal ticks and clicks. It has optional
correction-triggered resume/disable settings. Its instrumentation-dependent
behavior has not been runtime-verified for this build; it is not a stable
compatibility guarantee or a server-side pause.

## Requirements & Installation

Target: **Minecraft 1.8.9**, **Java 17**, and a compatible
[WeaveLoader](https://github.com/Weave-MC/Weave-Loader) environment. This version
compiles against **WeaveLoader/API 1.3.4**, using Weave Gradle 1.3.3; Stormy's old
Weave 0.2.4 setup is not the target. Lunar must run the matching Minecraft version
with a loader setup that supports it. Other Lunar builds/loader versions are
unverified; compilation and unit tests do not confirm in-game compatibility.

1. Obtain the JAR from [Veyra Releases](https://github.com/ZOONGG/Moonrise-Veyra/releases)
   once the owner publishes it manually, or build it below.
2. Close the game and copy the JAR into the mod directory used by your Weave
   setup (normally `%USERPROFILE%/.weave/mods`). For Moonrise, use its
   `packages/weave` directory or the explicit install task below.
3. Launch Minecraft 1.8.9 with the configured loader and press `Enter` in-game.

The build alone does not install or alter Lunar. A real in-game smoke check is
still required before declaring runtime compatibility.

## Build

Use JDK 17 and the included Gradle wrapper:

```powershell
.\gradlew.bat clean build
```

The artifact is written to `build/libs/Veyra-0.1.1-moonrise.1.jar`.
`build` includes the JUnit tests. CI builds and uploads workflow artifacts only;
GitHub Releases and tags are managed manually.

To build and copy it into a local Moonrise installation:

```powershell
.\gradlew.bat installToMoonrise -PmoonriseHome=D:\path\to\Moonrise
```

Alternatively, set the `MOONRISE_HOME` environment variable and run the same
task without `-PmoonriseHome`.

## Development layout

- `dev.veyra.client` contains the module, configuration, and UI layers.
- `dev.veyra.weave` contains Weave entrypoints, events, hooks, and mixins.
- Runtime configuration is stored under `.weave/veyra`, separately from Stormy.

## По-русски

Veyra — развиваемый Weave-мод для Minecraft 1.8.9 на основе Stormy.
Описание отличий, установки и ограничений: [README.ru.md](README.ru.md).

## Credits & Attribution

- Original code: [Tryflle/stormy](https://github.com/Tryflle/stormy), by Tryflle.
  Stormy's README credits an early [syz66/illusion](https://github.com/syz66/illusion)
  build (delisted), itself based on Raven. This historical attribution is retained.
- Independent Veyra work: the [Veyra project](https://github.com/ZOONGG/Moonrise-Veyra),
  including the interface, configuration, added modules and behavior changes above.
- Loader: [Weave-MC / WeaveLoader](https://github.com/Weave-MC/Weave-Loader).
- Bundled Inter font: Copyright 2020 The Inter Project Authors; SIL Open Font
  License 1.1 is retained in `src/main/resources/assets/veyra/fonts/OFL.txt`
  and packaged in the JAR.

Neither this repository nor the archived Stormy tree declares a project code
license. Attribution does not grant redistribution rights; the owner must
resolve upstream permission/licensing before publishing a public binary release.
No new license is assigned to inherited code here.
