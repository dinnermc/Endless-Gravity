# Changelog

## 1.1.9

- **Dedicated server support** — runs on a server without any extra client-side installs: cloth_config became an optional dependency and the config screen only registers when it's present

## 1.1.8

- **Removed the environment systems** — temperature (freezing) and oxygen (depletion, suffocation, tank recharge) are gone, together with their config options and the O₂ HUD / suffocation overlay
- **Removed the Stellar Suit and oxygen tanks** — the four armor pieces and their custom model, the portable oxygen tanks, the End city tank loot, and all related recipes are gone
- **Removed the custom creative tab** — the starship blocks and Dinner Plush now live in the vanilla *Functional Blocks* tab

## 1.1.7

- **Oxygen tanks in End citadel chests** - the End city treasure chests (towers and ships) now have a chance to spawn a portable oxygen tank: the Large (1000 O₂) is the common find, the regular tank (250 O₂) is much rarer
- **No fall damage in the high Overworld** - above the atmosphere base (Y 64) fall damage is negated: thin air makes impacts negligible; the low altitudes keep vanilla damage. Skipped entirely when Create: Cosmonautics is installed
- **Create: Cosmonautics integration** - when `rocketnautics` is present, Endless Gravity backs off the Overworld: no atmosphere gravity on players/entities, no sub-level levitation, the Sable Overworld datapack is removed and cold/oxygen are left to Cosmonautics. The End stays fully managed by Endless Gravity (vacuum, freezing, suffocation untouched)
- **Fix: no gravity in the End with Sable installed** - the vanilla End is managed by Endless Gravity's own reduced-gravity systems again (0.055 player / 0.025 item offsets); Sable's dimension_physics only drove the sub-levels, leaving the End entities falling at vanilla speed
- **Softer audio muffling in the End** - low-pass gain raised to 0.35 / 0.25, the dampened effect is subtler

## 1.1.6

- **Scaled suffocation** — without the Stellar Suit (or with it once the oxygen runs dry) the time before dying now scales with altitude: ~150s at Y 400, 90s at mid-altitude, down to a hard minimum of 30 seconds at deep space/The End. The screen-fade overlay matches the real timer
- **Vanilla fall damage in the Overworld** — fall damage is no longer intercepted above the atmosphere; only The End and Sable sub-levels use the configurable modes
- **Fix dedicated-server crash while a starship is flying** — the engine sound controller (a client-only class) was called from the server tick. It now lives entirely in the client source set and the server streams engine state through a `rocket_sound` packet to nearby players

## 1.1.5

- **Portable oxygen tanks** — carry an `Oxygen Tank` (250 O₂) or `Large Oxygen Tank` (1000 O₂) in your inventory: the Stellar Suit breathes from them first and keeps its chestplate tank topped up even when it isn't empty, then falls back to the suit reserve once they run dry. Both recharge for free at full atmosphere pressure and never break
- Oxygen tanks and the Stellar Chestplate now show a custom cyan O₂ bar (red when low) plus a numeric `O₂: X / Y` tooltip
- Fix items and projectiles flying upward in thin Overworld air — the atmosphere upward force is now capped at each entity's own gravity, so high-altitude items and arrows become weightless instead of accelerating upward

## 1.1.4

- The End is now a full vacuum: freezing and suffocation apply at every altitude (no height curve) — wear the Stellar Suit or die; the oxygen tank drains in The End and never recharges
- Freezing now only starts above Y 400 (atmosphere progress 0.5): the frost curve is remapped to 0 at Y 400 and ramps to max at deep space, so low-altitude builds never get random cold spikes

## 1.1.3

- Freezing is now deadly in space — frost damage scales with altitude and the time spent exposed (a single burst gone in seconds at deep space). Only the Stellar Suit can stop it
- Default max gravity offset lowered to 0.075: deep space keeps a tiny residual pull instead of perfect zero-G, so motion always slows down on its own

## 1.1.2

- Fix suffocation death loop on respawn — the suffocation timer is now cleared on death and respawn, so a player never dies the same tick they reappear
- Fix vanilla air bubbles no longer appear in the hotbar — the mod no longer manipulates the vanilla air meter
- Change oxygen suffocation now only starts in genuinely thin air (atmosphere progress ≥ 0.5, roughly Y 400+ with default layers), keeping the troposphere breathable so elevated spawns never cause a death loop
## 1.1.1

- Updated the Stellar Suit and its item textures
## 1.1.0

- **New config screen** — Cloth Config is now bundled (Jar-in-Jar, no manual install). Tabs for **All**, **The End**, **Overworld**, and **General**, color-coded entries, per-entry reset, and amber restart warnings where needed
- **Atmosphere rework** — the discrete layer/drag system was replaced by a single continuous pressure curve (8 configurable altitude → pressure layers, default `-64:1.25 … 3500:0.0`). Gravity, audio muffling, temperature, and oxygen now all derive from the same curve
- **Fall damage** — velocity-based mode now also applies in the Overworld; all three fall damage settings moved to a shared *General* tab
- **Starship** — two-block rocket (Super Heavy booster + Starship upper stage) with Sable rigid-body physics: liftoff thrust ramp, separation at Y ≥ 1800 (or 700 ticks), cruise/drift phase with lateral control, and powered landing. Engine startup/loop/shutdown sounds with 128-block distance falloff
- **Stellar Suit** — full four-piece armor set with custom model and texture; the chestplate carries a configurable oxygen tank (inventory bar, cyan >25%, red when low) that drains at altitude and recharges at full pressure; helmet + chestplate grant breathing in thin air
- **Dinner Plush** — a decorative, bouncy (Sable) plush that plays a toy sound when punched
- **Addon API rework** — gameplay events removed in favor of `EndlessGravityAPI` utilities (pressure, atmosphere progress, muffle/temperature/oxygen helpers, Sable detection, sub-level Y projection) and the `endless_gravity:gravity_immune` entity tag
- **Gravity engine refactor** — per-entity mixins replaced by unified tick handlers (`GravityHandler`, `EnvironmentHandler`); mixin surface reduced to the two client audio mixins
- **Audio** — per-player muffle state tracks the pressure curve; starship sounds no longer ring across the whole world
- **Sable** — datapack generated for The End (gravity Y −4, pressure 0, drag 0.05, priority 9999) and the Overworld (priority 2000); regenerated from the config screen when values change

## 1.0.25

- Properly register `endless_gravity.sable.mixins.json` in `neoforge.mods.toml`

## 1.0.24

- Reduce atmosphere drag default to 0.5 for slower entity acceleration

## 1.0.23

- Mixin for sub-level structure atmosphere gravity

## 1.0.22

- Fix drag compensation in Sable sub-levels; restore Overworld Sable datapack

## 1.0.21

- Weaker gravity at high altitude (0.08 max offset); atmosphere works in Sable sub-levels

## 1.0.20

- Mixin-based drag at source, with Sable compatibility

## 1.0.19

- Post-tick drag compensation — preserves gravity + input, covers players and mobs

## 1.0.18

- Fix sub-level inertia — project Y before velocity threshold; robust Sable dimension detection

## 1.0.17

- Full 3-axis drag compensation; inertia caps at Kármán line; realistic temperature/oxygen layers

## 1.0.16

- Full inertia at deep space — drag compensation cancels Minecraft's 0.91 drag

## 1.0.15

- Sable Overworld `dimension_physics` with pressure function; sub-level gravity fix

## 1.0.14

- Real atmospheric layer system

## 1.0.13

- Fix `ChannelMixin` crash on Channel resume

## 1.0.12

- Muffled audio fix; Sable sub-level gravity fix; config spacing

## 1.0.11

- More space-like audio; Sable priority set to maximum

## 1.0.10

- Sable sub-levels now get Overworld layer gravity, temperature, and oxygen

## 1.0.9

- Overworld enabled by default; temperature/oxygen systems; reduced max gravity; lang entries

## 1.0.8

- API improvements — immunity/application events, config sync, velocity/position getters

## 1.0.7

- Height-based muffled audio in Overworld space layers

## 1.0.6

- Compatibility and stability fixes

## 1.0.5

- Stability fixes

## 1.0.4

- Fix dedicated server crash

## 1.0.3

- Add addon API hooks; fix config screen clipping; update Maven publishing

## 1.0.2

- Fix Sable datapack loading; fall damage modes; audio filter optimization; config UI improvements
