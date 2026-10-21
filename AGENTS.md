# AGENTS.md - Grey Horizons RPG

NeoForge 1.21.1 Minecraft mod, multi-module Gradle project (root `build.gradle` is thin;
each subpack has its own `build.gradle`).

## Modules
- `Atmospheric_Overhaul_Subpack` - seasons, temperature, weather, wind, sky/cloud rendering
- `Farming_Overhaul_Subpack` - crops, clay pot, cutting board, fruit trees, spoilage
- `TerraForma_Subpack` - biomes, climate map, custom chunk generator
- `Frontier_Subpack` - title screen, settings, exotic spawns
- `Belliarium_Monstrarium_Subpack` - survival (hunger/thirst/temp/sanity), body health, medical items, backpacks, skill books, HUDs

All mod code lives under `net.enderwish.<SubpackName>`.

## Working rules for agents
- The user supplies design plans. For long task lists, ask clarifying questions first and
  keep asking until the intent is ~95% clear; then implement and verify.
- Code from verified facts, not guesses. Ground truth for Minecraft internals is the
  decompiled source in `.abacusai/temp/mcsrc` (read it before relying on a method signature).
- Build after every change set: `gradlew.bat :<Subpack>:build` (or `gradlew.bat build` for all).
- Commit after each working step; never push.

## Dev dashboard (side task bar)
A live dashboard lists the current task and the files edited:
- URL: http://localhost:7645
- Restart: `powershell -ExecutionPolicy Bypass -File devtools/dashboard/start.ps1`
- Task/file list to edit: `devtools/dashboard/state.json`
- The "files in working tree" table is read live from git; "files touched this session"
  comes from `state.json`.
