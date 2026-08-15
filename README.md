<div align="center">
  <img src="src/main/resources/icon.png" alt="Endless Gravity Icon" width="180" />

  <h1>Endless Gravity</h1>

  <p>
    <a href="https://modrinth.com/mod/endless-gravity">
      <img src="https://img.shields.io/modrinth/dt/endless-gravity?logo=modrinth&label=Downloads&style=flat&color=242629&labelColor=5CA424&logoColor=fff" alt="Modrinth Downloads" />
    </a>
    <img src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen?logo=minecraft" alt="Minecraft Version" />
    <img src="https://img.shields.io/badge/Loader-NeoForge-orange" alt="NeoForge" />
    <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License" />
  </p>

  <p>Low gravity for <b>The End</b> and layered atmosphere physics for the <b>Overworld</b>.</p>
</div>

## Features

- **The End**: reduced gravity for players, items, arrows, thrown projectiles, and falling blocks, configurable per entity type
- **Overworld atmosphere**: a continuous pressure curve (8 configurable layers) weakens gravity and muffles audio above Y 64, up to full vacuum at Y 3500
- **Fall damage** — normal or disabled in The End and Sable sub-levels, and no fall damage above a configurable altitude in the Overworld
- **Sable integration** — physics datapack (gravity, pressure, drag) generated automatically for The End and the Overworld, with configurable priorities
- **Modded config screen** — Cloth Config (bundled, no extra install) with *All*, *The End*, *Overworld*, and *General* tabs
- **Addon API** — `EndlessGravityAPI` utility (pressure, atmosphere progress, real Y projection, Sable helpers) plus the `endless_gravity:gravity_immune` entity type tag
- **Compatible** with any modded dimension or Sable sub-level; config is synced from server to client on login

## Configuration

Open the mod settings from the NeoForge mod list (or *Config → Endless Gravity*). Changes apply live where possible; the audio filter and Sable datapack need a restart.

### The End

| Setting | Default | Description |
|---|---|---|
| End Gravity | ON | Master toggle for low gravity in The End |
| Player Gravity Offset | 0.055 | Upward force per tick; 0.055 floats gently, crank it and you barely fall |
| Item Gravity Offset | 0.025 | Upward force per tick for items (they can't steer, keep it low) |
| Arrow Gravity Offset | 0.03 | Upward force per tick for arrows and tridents |
| Thrown Projectile Offset | 0.018 | Upward force per tick for snowballs, potions, pearls |
| Falling Block Offset | 0.035 | Upward force per tick for sand, gravel, anvils, dragon eggs |
| Fall Damage Mode | Normal | Normal / Disabled (The End and Sable sub-levels; the Overworld stays vanilla) |
| Audio Filter Gain | 0.35 | Low-pass volume in The End; 1.0 is vanilla audio |
| Audio Filter Gain HF | 0.25 | High-frequency side of the filter |

### Overworld

| Setting | Default | Description |
|---|---|---|
| Enable Atmosphere | ON | Master toggle for Overworld space effects |
| Entity Gravity | ON | Gravity from the pressure curve applies to entities |
| Max Gravity Offset | 0.075 | Upward force at full vacuum (progress × max) |
| No Fall Damage Above | 400 | Above this Y, fall damage is cancelled (64–3500) |
| Muffle Gain | 0.01 | Low-pass gain at full vacuum, interpolated from 1.0 at ground |
| Muffle Gain HF | 0.005 | High-frequency part of the muffle |
| Atmosphere Layers | 8 layers | Altitude → pressure pairs: `-64:1.25, 64:1.0, 400:0.5, 900:0.2, 1200:0.08, 1800:0.01, 2500:0.001, 3500:0.0` |

## Addon API

`EndlessGravityAPI` (Java, `dinner.dev.endless_gravity.EndlessGravityAPI`) provides:

- `getPressure(y)` / `getAtmosphereProgress(y)` — read the configured pressure curve
- `getAtmosphereOffset(y)`, `getAtmosphereMuffleGain(y)`, `getAtmosphereMuffleGainHF(y)` — derived atmosphere values
- `getRealY(entity)`, `getRealY(level, pos)` — project positions out of Sable sub-levels into global space
- `isSableLoaded()`, `isSableManaged(level)`, `isOverworldOrSable(level)` — dimension/Sable helpers
- `GRAVITY_IMMUNE` tag — add entity types to `data/endless_gravity/tags/entity/gravity_immune.json` to make them ignore low gravity in The End

All gameplay-affecting config values are synced from server to client on login via `ConfigSyncPayload`.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1+
- Cloth Config — bundled (via Jar-in-Jar), no manual install
- Sable (optional — the mod runs without it; only the Sable sub-level physics and datapacks are skipped)
- Create: Cosmonautics (optional — when installed, it owns Overworld gravity and Endless Gravity backs off)

## License

[Apache 2.0](LICENSE)
