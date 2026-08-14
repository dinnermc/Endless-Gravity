# Changelog

## 1.1.9

- **Dedicated server support** — runs on a server without any extra client-side installs: cloth_config became an optional dependency and the config screen only registers when it's present

## 1.1.8

- removed the temperature/oxygen systems entirely: Stellar Suit, O2 tanks, HUD, config... all gone
- starship and plush now live in the vanilla functional blocks tab

## 1.1.7

- oxygen tanks in end citadel chests (large one common, regular one rare)
- no fall damage in the high overworld (Y 64+); skipped entirely when Cosmonautics is installed
- Create: Cosmonautics integration: the mod backs off the overworld and sub-levels when present
- fix: the end lost its reduced gravity with Sable installed, floats again
- softer end muffling (gains 0.35/0.25)

## 1.1.6

- suffocation scaled by altitude: ~150s at Y 400, 30s minimum in deep space
- vanilla fall damage back in the overworld
- fix dedicated server crash with the starship flying: engine sound now goes through a packet

## 1.1.5

- portable oxygen tanks (250 / 1000 O2) that recharge for free at full pressure
- cyan O2 bar on the tank and the Stellar Suit chestplate
- fix items and arrows flying up in thin air (lift capped at each entity's own gravity)

## 1.1.4

- the end is a full vacuum now: freezing and suffocation at any altitude, no height curve
- freezing only above Y 400 (frost curve remapped)

## 1.1.3

- freezing deadly in space: scales with altitude and exposure time
- max gravity offset default 0.075, deep space keeps residual pull

## 1.1.2

- fix suffocation death loop on respawn
- fix vanilla air bubbles missing from the hotbar
- suffocation only in genuinely thin air (progress >= 0.5, ~Y 400+ with default layers)

## 1.1.1

- new Stellar Suit textures

## 1.1.0

- new config screen with bundled Cloth Config: All / The End / Overworld / General tabs
- atmosphere rework: continuous 8-layer pressure curve instead of discrete layers
- velocity-based fall damage also in the overworld
- starship: 2-block rocket with Sable physics, separation at Y 1800, powered landing with lateral control
- full Stellar Suit (4 pieces, custom model, oxygen tank in the chestplate)
- decorative bouncy Dinner Plush
- addon API: utilities instead of events, gravity_immune tag
- gravity engine refactor: unified tick handlers, fewer mixins
- per-player audio following the pressure curve
- Sable datapack for the end and overworld, regenerated from the config screen

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
