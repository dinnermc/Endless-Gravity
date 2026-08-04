# Changelog

## 1.1.0 (2026-08-04)

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
