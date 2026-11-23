<div align="center">
  <img src="src/main/resources/assets/endless_gravity/icon.png" alt="Endless Gravity Icon" width="180" />

  <h1>Endless Gravity</h1>

  <p>
    <a href="https://modrinth.com/mod/endless-gravity">
      <img src="https://img.shields.io/modrinth/dt/endless-gravity?logo=modrinth&label=Downloads&style=flat&color=242629&labelColor=5CA424&logoColor=fff" alt="Modrinth Downloads" />
    </a>
    <img src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen?logo=minecraft" alt="Minecraft Version" />
    <img src="https://img.shields.io/badge/Loader-Fabric-blue" alt="Fabric" />
    <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License" />
  </p>

  <p>Low gravity for <b>The End</b> and layered atmosphere physics for the <b>Overworld</b>.</p>
</div>

## Features

- **Per-dimension profiles**: configure The End, the Overworld, and modded dimensions as atmosphere or fixed-gravity profiles from the config screen (with quick dimension picker), `config/endless_gravity/dimensions.json`, or datapacks (`data/<namespace>/endless_gravity/dimensions/*.json`)
- **Fixed gravity**: independent offsets for players, mobs, items, arrows, thrown projectiles, falling blocks, primed TNT, and XP orbs
- **Atmosphere presets and curve editor**: choose a preset, set optional surface gravity, pressure-based air drag, and item/projectile scaling, and drag pressure/altitude points with live gravity curve and altitude markers
- **Fall damage**: normal, proportional (scales safe fall height and damage with effective gravity), or disabled — configurable globally and per dimension
- **Potions & HUD**: brewable *Weightlessness* (Awkward + Shulker Shell) and *Heaviness* (Awkward + Iron Ingot) potions (including Long with Redstone and Level II with Glowstone), plus a minimalist Gravity & Pressure HUD (standard or compact) with toggle keybind
- **Native config screen**: built-in two-pane minimalist UI with zero config library dependencies (install Mod Menu to open it in-game), dimension filter, interactive curve editor, and one-click *Copy JSON* datapack export
- **Addon API & datapacks**: stable `GravityApi` (entity and position-based queries, biome/biome-tag gravity overrides via `data/<ns>/endless_gravity/biomes/*.json`, client audio-muffle modifiers via `registerAudioMuffleModifier`, multiplicative `scaleGravity`, priority-ordered modifiers) plus the `endless_gravity:gravity_immune` tag
- **Compatible** with modded dimensions; gameplay config, dimension profiles, and the `endlessGravity` game rule are synced from server to client on login and reload

## Configuration

Open the mod settings from Mod Menu (or *Config → Endless Gravity*). In **Dimensions**, Overworld and End profiles are created by default. To add another, enter its ID (for example `minecraft:the_nether`), choose a mode, apply an optional preset, and customize the settings. Atmosphere layers can be adjusted by dragging points or entering altitude and pressure values; hover over the graph to read the curve and cursor values. Add and remove layers directly in the editor (at least two are required). Removing a profile is reversible until you save; **Undo** restores it. All changes are written when you press **Save**.

Save atmosphere profiles as named presets in the editor. The *Custom atmosphere presets* section imports or exports presets as JSON text; export copies the JSON to the clipboard. The preset library is stored at `config/endless_gravity/atmosphere_presets.json`.

Each profile uses one mode:

| Mode | Settings | Entities affected |
|---|---|---|
| `fixed_gravity` | Per-type gravity offsets for players, mobs, items, arrows, thrown projectiles, and blocks | Each listed type |
| `atmosphere` | Start altitude, maximum offset, pressure curve, audio gains, and fall-damage threshold | Players and mobs |

Profiles are stored in `config/endless_gravity/dimensions.json`. Existing legacy files are read and migrated to the profile format when saved. Positive fixed offsets reduce gravity; zero keeps vanilla gravity. Set `no_fall_damage_above` to `null` to leave fall damage unchanged at every altitude.

The **General** tab contains global fall-damage, End-gravity, atmosphere-enable, audio-filter, potion, HUD, and gravity-immunity settings. The atmosphere starts reducing gravity at Y=319 by default and reaches its maximum at the final curve point.

All gameplay config values, dimension profiles (`dimensions.json` and datapacks), and the `endlessGravity` game rule are synced from server to client on login and reload via `ConfigSyncPayload`.

## Addon API

The stable public API is `dinner.dev.endless_gravity.api.GravityApi`. Everything in the
`dinner.dev.endless_gravity.api` package is versioned by `GravityApi.API_VERSION`; classes outside it
(`EndlessGravityAPI`, config, mixins) are internal and may change without notice.

- `getGravityOffset(entity)` / `getGravityOffset(level, pos, channel)`: the upward offset the mod would apply (`0.0` if none), for a live entity or any world position
- `getGravityContext(entity)` / `channelFor(entity)`: the full calculation (source, offset, `baseGravity()`, `effectiveGravity()`, `scaleGravity(factor)`, cancellation) and the entity's gravity channel (`PLAYER`, `MOB`, `ITEM`, `ARROW`, `THROWN`, `BLOCK`)
- `getPressure(level, y)`, `getAtmosphereProgress(level, y)`, `getAtmosphereOffset(level, y)`, `getRealY(entity)`: read dimension-specific or Overworld atmosphere curves and world-space entity altitude
- `registerModifier(id, priority, modifier)` / `getRegisteredModifiers()`: run custom logic whenever an offset is resolved; `ctx.setOffset`, `ctx.addOffset`, `ctx.scaleGravity`, `ctx.cancel`, and `ctx.uncancel` control the result. Higher priority runs first (ties broken deterministically by ID), and a per-entity modifier (`setEntityModifier`) runs last
- `GRAVITY_IMMUNE` tag: add entity types to `data/endless_gravity/tags/entity_type/gravity_immune.json` to make them ignore reduced gravity
- **Datapack profiles**: place JSON files under `data/<namespace>/endless_gravity/dimensions/<path>.json` to ship built-in gravity/atmosphere defaults for custom dimensions (user `config/endless_gravity/dimensions.json` always overrides datapacks)

### Depending on the API

The artifact is on the Modrinth Maven as `maven.modrinth:endless-gravity` (`1.3.3` for NeoForge, `1.3.3-1.21.1-fabric` for Fabric); see the [Modrinth Maven docs](https://docs.modrinth.com/docs/tutorials/maven/) to add the repository. Declare it as a dependency (`"endless_gravity": ">=1.3.0"` in `fabric.mod.json`, or a `[[dependencies.mymod]]` entry with `versionRange = "[1.3.0,)"` in `neoforge.mods.toml`).

```java
GravityApi.registerModifier(ResourceLocation.fromNamespaceAndPath("mymod", "moon"), 0, ctx -> {
    if (ctx.level().dimension().location().toString().equals("mymod:moon")) {
        ctx.addOffset(0.02); // a little extra lift on top of the mod's own resolution
    }
});
```

Gameplay config values, dimension profiles, and the `endlessGravity` game rule are synced from server to client on login and reload via `ConfigSyncPayload`.

Client-only settings are **not** synced and always save locally even while connected to a remote server: the End audio filter, particle gravity, and Gravity HUD settings. While connected to a remote server, synced gameplay values are applied in memory for that session and your own values are restored on disconnect.

## Loader differences

Both builds share the same config and behavior for The End, the Overworld, and custom dimensions. These are the only known gaps:

| Feature | NeoForge | Fabric |
|---|---|---|
| Create: Cosmonautics integration | Overworld gravity backs off when it is installed | Not supported |
| Sable sub-level physics and datapacks | Supported | Not supported |

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.10+
- Fabric API
- Mod Menu — optional, provides the button to open the built-in config screen
- Create: Cosmonautics and Sable — **not supported on this build** (see *Loader differences*)

## License

[Apache 2.0](LICENSE)
