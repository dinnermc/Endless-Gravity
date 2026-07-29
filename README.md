# Endless Gravity

Low gravity for **The End** and layered gravity for the **Overworld**. Jump higher, fall slower, float longer.

## Features

- **Reduced gravity** for players, items, arrows, thrown projectiles, and falling blocks in The End — fully configurable per entity type
- **Overworld layered gravity** — enable gravity layers above a configurable Y level with continuous (non-discrete) force, auto-applied to Sable sub-levels
- **Fall damage** — three modes: normal, disabled, or velocity-based with configurable scale and minimum velocity
- **Audio filters** — muffled low-pass sound effect in The End, plus height-based muffling in Overworld space layers (interpolated from clear at ground to muffled at max altitude)
- **Particle gravity** — particles drift slower in low-gravity zones
- **Sable integration** — optional physics datapack (gravity Y, pressure, drag) auto-enabled on new worlds
- **Addon API** — four events (`GravityImmunityEvent`, `GravityApplicationEvent`, `GravityAppliedEvent`, `FallDamageCalculationEvent`) for addons to override, cancel, or react to gravity; server→client config sync via network payload
- **Compatible** with any modded dimension or sub-level

## Configuration

Open the mod settings from the NeoForge mod list. All values can be toggled and adjusted per-option.

### The End

| Setting | Default | Description |
|---|---|---|
| Player Gravity Offset | 0.055 | Upward force per tick. Higher = less gravity |
| Item Gravity Offset | 0.025 | Upward force per tick for items |
| Arrow Gravity Offset | 0.03 | Upward force per tick for arrows/tridents |
| Thrown Projectile Offset | 0.018 | Upward force per tick for thrown projectiles |
| Falling Block Offset | 0.035 | Upward force per tick for sand, gravel, anvils, dragon eggs |
| Particle Multiplier | 0.3 | 0 = no gravity, 1 = vanilla |
| Fall Damage Mode | Velocity-Based | Normal / Disabled / Velocity-Based |
| Audio Filter Gain | 0.4 | Low-pass filter volume (lower = more muffled) |
| Audio Filter Gain HF | 0.3 | Low-pass filter high-frequency volume |

### Overworld Layers

| Setting | Default | Description |
|---|---|---|
| Enable Overworld Gravity | false | Enable layered gravity above a Y threshold |
| Start Y | 1000 | Y level where layers begin |
| Layer Height | 500 | Blocks per layer |
| Max Layers | 4 | Maximum layers before plateau |
| Force Per Layer | 0.02 | Continuous upward force per layer (clamped to 0.08) |
| Muffle Gain | 0.2 | Low-pass gain at max layers (interpolated from 1.0 at ground) |
| Muffle Gain HF | 0.1 | Low-pass high-frequency gain at max layers |

### Sable Integration

If [Sable](https://modrinth.com/mod/sable) is installed, Endless Gravity generates a datapack with custom physics for The End. Configure gravity, pressure, drag, and priority from the mod settings.

## Addon API

Endless Gravity provides four events on `NeoForge.EVENT_BUS`:

| Event | Cancellable | When | Use case |
|---|---|---|---|
| `GravityImmunityEvent` | Yes | Before the tag-based immunity check | Grant/deny temporary immunity per tick |
| `GravityApplicationEvent` | Yes | Before gravity is applied | Override offset or cancel entirely |
| `GravityAppliedEvent` | No | After gravity is applied | React to the applied offset |
| `FallDamageCalculationEvent` | Yes | When calculating velocity-based fall damage | Override damage multiplier/distance |

All gameplay-affecting config values are synced from server to client on login via `ConfigSyncPayload`.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1+
- Sable (optional)

## License

[Apache 2.0](LICENSE)
