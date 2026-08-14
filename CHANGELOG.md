# Changelog

## 1.1.9

- **Dedicated server support** — runs on a server without any extra client-side installs: cloth_config became an optional dependency and the config screen only registers when it's present

## 1.1.8

- quité el sistema de temperatura y oxigeno entero: Stellar Suit, tanques de O2, HUD, config... todo fuera
- starship y plush ahora en la tab vanilla de bloques funcionales

## 1.1.7

- tanques de oxigeno en los cofres de las end citadels (el grande es comun, el normal raro)
- sin fall damage en el Overworld alto (Y 64+); se salta entero si Cosmonautics esta instalado
- integracion con Create: Cosmonautics: si esta, el mod se aparta del Overworld y de los sub-levels
- fix: con Sable instalado el End habia perdido la gravedad reducida, vuelve a flotar
- muffling del End mas suave (gains 0.35/0.25)

## 1.1.6

- asfixia escalada por altitud: ~150s a Y 400, minimo 30s en espacio profundo
- fall damage vanilla otra vez en el Overworld
- fix crash del servidor dedicado con la starship volando: el sonido del motor ahora va por paquete

## 1.1.5

- tanques de oxigeno portatiles (250 / 1000 O2) que se recargan gratis a presion atmosferica
- barra de O2 cyan en el tanque y la chestplate del Stellar Suit
- fix items y flechas volando para arriba en el aire fino (el lift se capa con la gravedad de cada entity)

## 1.1.4

- el End es vacio total: congelacion y asfixia a cualquier altitud, sin curva de altura
- congelacion solo por encima de Y 400 (curva de frost remapeada)

## 1.1.3

- congelacion mortal en el espacio: escala con altitud y con el tiempo expuesto
- max gravity offset por defecto a 0.075, el espacio profundo mantiene pull residual

## 1.1.2

- fix death loop de asfixia al respawnear
- fix burbujas de aire vanilla que habian desaparecido
- asfixia solo en aire fino de verdad (progress >= 0.5, ~Y 400+ con las capas por defecto)

## 1.1.1

- texturas nuevas del Stellar Suit

## 1.1.0

- config screen nuevo con Cloth Config bundled: tabs All / The End / Overworld / General
- atmosfera rework: curva de presion continua de 8 capas en vez del sistema de capas discretas
- fall damage por velocidad tambien en el Overworld
- starship: cohete de 2 bloques con fisica de Sable, separacion a Y 1800, aterrizaje con control lateral
- Stellar Suit completo (4 piezas, modelo custom, tanque de oxigeno en la chestplate)
- Dinner Plush decorativo y saltarin
- Addon API: utilidades en vez de eventos, tag gravity_immune
- refactor del motor de gravedad: tick handlers unificados, menos mixins
- audio por jugador siguiendo la curva de presion
- datapack de Sable para The End y Overworld, regenerable desde la config

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
