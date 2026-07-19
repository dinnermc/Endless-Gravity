# Endless Gravity

Low gravity for The End. Jump higher, fall slower, float longer.

## Features

- **Reduced gravity** for players, items, arrows, thrown projectiles, and falling blocks in The End
- **Fall damage modes**: normal, disabled, or velocity-based
- **Audio filter**: muffled low-pass sound effect in The End
- **Particle gravity**: particles drift slower in The End
- **Sable integration** (optional): physics-based gravity, pressure, and drag via datapack, auto-enabled on new worlds

## Configuration

Open the mod settings from the NeoForge mod list. All values can be toggled and adjusted per-option.

| Setting | Default | Description |
|---|---|---|
| Player Gravity Offset | 0.055 | Upward force per tick. Higher = less gravity |
| Item Gravity Offset | 0.025 | Upward force per tick for items |
| Arrow Gravity Offset | 0.03 | Upward force per tick for arrows/tridents |
| Thrown Projectile Offset | 0.018 | Upward force per tick for thrown projectiles |
| Falling Block Offset | 0.035 | Upward force per tick for sand, gravel, anvils, dragon eggs |
| Particle Multiplier | 0.3 | 0 = no gravity, 1 = vanilla |
| Fall Damage Mode | Disabled | Normal / Disabled / Velocity-Based |

### Sable Integration

If [Sable](https://modrinth.com/mod/sable) is installed, Endless Gravity generates a datapack with custom physics for The End. Configure gravity, pressure, drag, and priority from the mod settings.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1+
- Sable (optional)

## License

[Apache 2.0](LICENSE)
