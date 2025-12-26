# DropMultiplier (Fabric, Minecraft 1.21.10)

A simple Fabric mod that multiplies drops from:
- Block loot tables (breaking blocks)
- Entity loot tables (killing mobs)

Default multiplier is **2x**. You can override per block or per entity.

## Requirements
- Minecraft Java Edition 1.21.10
- Fabric Loader
- Fabric API
- Java 21+

## Build
- Windows: `gradlew.bat build`
- macOS/Linux: `./gradlew build`

Built jar: `build/libs/dropmultiplier-1.0.0.jar`

## Mod Menu GUI

This mod exposes a config screen in Mod Menu when Mod Menu is installed.

Config screen is available via Mod Menu (optional).

## Config
File is created on first run:
`config/dropmultiplier.json`

Example:
```json
{
  "defaultMultiplier": 2,
  "blockMultipliers": {
    "minecraft:oak_log": 5
  },
  "entityMultipliers": {
    "minecraft:zombie": 3
  }
}
```

Notes:
- This mod multiplies item stack counts produced by loot tables.
- If a multiplied stack would exceed the item's max stack size, it is capped (no extra stacks are created).

## Commands
Requires permission level 2 (singleplayer is fine).

- Reload config:
  `/dropmultiplier reload`

- Set default:
  `/dropmultiplier set default <multiplier>`

- Set per-block:
  `/dropmultiplier set block <block_id> <multiplier>`

- Set per-entity:
  `/dropmultiplier set entity <entity_id> <multiplier>`

- Remove overrides:
  `/dropmultiplier remove block <block_id>`
  `/dropmultiplier remove entity <entity_id>`
