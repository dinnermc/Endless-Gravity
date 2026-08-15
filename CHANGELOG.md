# Changelog

## 1.2.5

- fall damage is disabled by default in the end and sable sub-levels

## 1.2.4

- removed the velocity-based fall damage mode: it never actually triggered, the impact speed reads as zero by the time the event fires, so the end uses vanilla fall damage by default again (or disabled, if you prefer)
- added the Spanish (Castilian) translation

## 1.2.3

- the end and overworld master toggles finally sync to clients (thanks Spagles)
- duplicate altitudes in the atmosphere curve no longer produce NaN gravity (thanks Spagles)
- atmospheric audio only muffles in the overworld and sable sub-levels, not the nether (thanks Spagles)
- the OpenAL filter re-creates itself cleanly if the audio device reloads (thanks Spagles)

## 1.2.2

- no fall damage above Y=400 in the overworld (configurable), so falls from way up high don't hurt anymore
- atmosphere now has a noFallDamageAltitude setting, synced to clients like the rest

## 1.2.1

- overworld fall damage is vanilla again at every height (the no-damage-above-Y-64 thing is gone)
- fixed the config deprecation warning for defineList

## 1.2.0

- removed the starship and the dinner plush: blocks, items, recipes, sounds and physics properties all gone
- sable is now truly optional, the mod runs without it (only the sable sub-level physics and datapacks are skipped)

## 1.1.9

- dedicated server support: works with only the server installed, cloth_config is optional now and the config screen only registers when its present

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

- actually register the sable mixins config in the mods toml, it was missing

## 1.0.24

- atmosphere drag default down to 0.5, entities accelerate slower

## 1.0.23

- sub-levels get atmosphere gravity via mixin

## 1.0.22

- fix drag compensation in sable sub-levels, restore the overworld datapack

## 1.0.21

- weaker gravity up high (0.08 max offset), atmosphere also works in sub-levels

## 1.0.20

- drag moved to a mixin at the source, works with sable

## 1.0.19

- post-tick drag compensation, keeps gravity and input, covers players and mobs

## 1.0.18

- fix sub-level inertia (project Y before the velocity check), sturdier sable dimension detection

## 1.0.17

- 3-axis drag compensation, inertia caps at the karman line, temperature and oxygen layers

## 1.0.16

- full inertia in deep space, drag compensation cancels the vanilla 0.91 drag

## 1.0.15

- sable overworld dimension_physics with a pressure function, sub-level gravity fix

## 1.0.14

- real atmospheric layer system

## 1.0.13

- fix `ChannelMixin` crash on Channel resume

## 1.0.12

- muffled audio fix, sable sub-level gravity fix, config spacing

## 1.0.11

- more space-like audio, sable priority maxed

## 1.0.10

- sub-levels inherit overworld layer gravity, temperature and oxygen

## 1.0.9

- overworld on by default, temperature and oxygen systems, lower max gravity, lang entries

## 1.0.8

- api improvements: immunity/application events, config sync, velocity and position getters

## 1.0.7

- height-based muffled audio in overworld space layers

## 1.0.6

- compatibility and stability fixes

## 1.0.5

- stability fixes

## 1.0.4

- fix dedicated server crash

## 1.0.3

- addon api hooks, fixed config screen clipping, maven publishing updated

## 1.0.2

- sable datapack loading fixed, fall damage modes, audio filter optimization, config ui improvements
