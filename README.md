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

- **Per-dimension profiles**: configure The End, the Overworld, and modded dimensions as atmosphere or fixed-gravity profiles
- **Fixed gravity**: independent offsets for players, mobs, items, arrows, thrown projectiles, and falling blocks
- **Atmosphere presets and curve editor**: choose a preset, then drag pressure/altitude points to customize the profile
- **Fall damage**: normal or disabled in The End, and no fall damage above a configurable altitude in the Overworld
- **Modded config screen**: *Dimensions* editor and *General* settings; add, remove, restore, and edit profiles without leaving the screen (requires Mod Menu and Cloth Config)
- **Addon API**: stable `GravityApi` (query the gravity the mod would apply, register modifiers to change or cancel it) plus the `endless_gravity:gravity_immune` entity type tag
- **Compatible** with modded dimensions; gameplay config is synced from server to client on login

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

Dimension profiles are stored locally and are not included in the server config-sync packet. For multiplayer, configure the same `dimensions.json` on the server and clients; ordinary synced settings continue to come from the server.

## Addon API

The stable public API is `dinner.dev.endless_gravity.api.GravityApi`. Everything in the
`dinner.dev.endless_gravity.api` package is versioned by `GravityApi.API_VERSION`; classes outside it
(`EndlessGravityAPI`, config, mixins) are internal and may change without notice.

- `getGravityOffset(entity)`: the upward offset the mod would apply this tick (`0.0` if none), resolved through the exact same code path the mod itself uses
- `getGravityContext(entity)` / `channelFor(entity)`: the full calculation (source, offset, cancellation) and the entity's gravity channel (`PLAYER`, `MOB`, `ITEM`, `ARROW`, `THROWN`, `BLOCK`)
- `getPressure(y)`, `getAtmosphereProgress(y)`, `getAtmosphereOffset(y)`, `getRealY(entity)`: read the configured atmosphere curve and world-space entity altitude
- `registerModifier(id, priority, modifier)`: run custom logic whenever an offset is resolved (the "calculate gravity" event); `ctx.setOffset`, `ctx.addOffset` and `ctx.cancel` control the result. Higher priority runs first, and a per-entity modifier (`setEntityModifier`) runs last
- `GRAVITY_IMMUNE` tag: add entity types to `data/endless_gravity/tags/entity_type/gravity_immune.json` to make them ignore reduced gravity

### Depending on the API

The artifact is on the Modrinth Maven as `maven.modrinth:endless-gravity` (`1.3.2` for NeoForge, `1.3.2-1.21.1-fabric` for Fabric); see the [Modrinth Maven docs](https://docs.modrinth.com/docs/tutorials/maven/) to add the repository. Declare it as a dependency (`"endless_gravity": ">=1.3.0"` in `fabric.mod.json`, or a `[[dependencies.mymod]]` entry with `versionRange = "[1.3.0,)"` in `neoforge.mods.toml`).

```java
GravityApi.registerModifier(ResourceLocation.fromNamespaceAndPath("mymod", "moon"), 0, ctx -> {
    if (ctx.level().dimension().location().toString().equals("mymod:moon")) {
        ctx.addOffset(0.02); // a little extra lift on top of the mod's own resolution
    }
});
```

Global gameplay config values are synced from server to client on login via `ConfigSyncPayload`.

Some settings are **not** synced and always come from your local config: atmospheric audio muffling, particle gravity, and custom dimensions (`config/endless_gravity/dimensions.json`). While connected to a remote server, synced global values are applied in memory for that session and your own values are restored on disconnect — synced values are never written to your config file.

## Loader differences

Both builds share the same config and behavior for The End and the Overworld. These are the only known gaps:

| Feature | NeoForge | Fabric |
|---|---|---|
| Create: Cosmonautics integration | Overworld gravity backs off when it is installed | Not supported |
| Sable sub-level physics and datapacks | Supported | Not supported |
| Custom dimensions — fixed gravity | All supported entity types | All supported entity types |

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.10+
- Mod Menu and Cloth Config — optional dependencies, both required for the in-game config screen
- Create: Cosmonautics and Sable — **not supported on this build** (see *Loader differences*)

## License

[Apache 2.0](LICENSE)
