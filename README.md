# Veyra

Veyra is an independently evolving Weave client for Minecraft 1.8.9. The
project starts from the archived [Tryflle/stormy](https://github.com/Tryflle/stormy)
codebase and is being separated into its own identity, configuration, design
system, and feature roadmap.

Veyra is under active reconstruction. Existing modules are retained as a
behavioral baseline while the UI and internals are replaced incrementally.
The ClickGUI key is `Enter`.

## Build

Use JDK 17 and the included Gradle wrapper:

```powershell
.\gradlew.bat clean build
```

The development artifact is written to `build/libs/Veyra-0.1.0.jar`.

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

Veyra — отдельный развиваемый Weave-мод для Minecraft 1.8.9. Сейчас проект
проходит поэтапную переработку: сначала отделяются идентификаторы и конфиги,
затем будет полностью заменён интерфейс и после этого будут перерабатываться
существующие модули и добавляться новые.
