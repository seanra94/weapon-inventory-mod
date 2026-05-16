# Public Release Checklist

Status: private
Scope: Curating the private Weapons Procurement repo into a public GitHub repo/package for `https://github.com/Shattersphere-Mods`
Last updated: 2026-05-17

Never publish this file. It describes private-to-public export rules for keeping the public mod focused on the procurement GUI and free of private agent/archive material.

## Release Target

- Public organization: `Shattersphere-Mods`
- Intended public product: clean Weapons Procurement GUI only.
- Private source of truth: `D:\Sean Mods\Weapons Procurement`
- Public release output must not be a blind mirror of this private repo.

## Current Public-Release Position

The private repo still contains optional patched badge and bytecode-patcher material. That material can stay private, but the public output must exclude it unless the user explicitly approves a different release strategy.

Before public export, use:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\export-public.ps1
```

The script writes a curated public tree to `build/public-export` by default and runs the leak scan below.

Do not push this private repo wholesale to the public organization.

## Public Include Surface

Public output should include only files needed for users/contributors to build and use the clean procurement GUI:

- `src/` after badge-only Java references are physically removable;
- Gradle wrapper and Kotlin build files required to compile the public source tree;
- `data/campaign/rules.csv`;
- public-safe `data/config/LunaSettings.csv`;
- `data/config/weapons_procurement_market_blacklist.json`;
- `data/config/weapons_procurement_stock.json`;
- `jars/weapons-procurement.jar` if the public repo ships the built jar;
- `mod_info.json`;
- `README.md`;
- `CONFIG.md`;
- `CHANGELOG.md`;
- user-facing build/package docs that do not mention private paths, agents, archives, or badge/bytecode systems;
- public-safe `.github/workflows/` if it does not require private paths or badge-only validators;
- normal build scripts required for public contributors.

## Public Exclude Surface

Exclude private and agent material:

- `AGENTS.md`;
- `.agent/`;
- `HANDOVER.md`;
- `PLANS.md`;
- `LESSONS.md`;
- archive/deep-dive/history docs;
- deploy queues, local logs, backups, build caches, and machine-specific files.

Exclude optional patched badge / bytecode material:

- `src/privateBadge/`;
- `src/weaponsprocurement/internal/WeaponsProcurementBadgeHelper.java`;
- `src/weaponsprocurement/internal/WeaponsProcurementCountUpdater.java`;
- any public-safe source references to those classes;
- `tools/cargo-stack-view-patcher.ps1`;
- `tools/validate-cargo-stack-view-patch.ps1`;
- `tools/validate-total-badges.ps1`;
- `tools/generate-total-badges.ps1`;
- `tools/patcher/`;
- `graphics/ui/wp_total_*.png`;
- badge sprite registry entries in `data/config/settings.json`;
- Luna setting `wp_enable_patched_badges`;
- docs mentioning `CargoStackView`, `starfarer_obf.jar`, bytecode injection, patched badges, badge helpers, or badge sprites.

## Required Private-To-Public Transformations

Resolved:

- `WeaponsProcurementModPlugin` uses a generic optional extension hook rather than direct badge updater imports.
- Badge helpers are physically separated under `src/privateBadge`.
- The private Gradle badge source set is stripped from public export output.
- Public config omits `wp_enable_patched_badges`.
- `data/config/settings.json` is treated as badge-only and omitted from public output.
- Public docs describe only the clean GUI product.
- `tools/export-public.ps1` curates public output and runs a leak scan.

Still open:

- Build the exported public tree and confirm the resulting jar contains no badge classes.
- Run `tools/validate-kotlin-migration.ps1` after public export to confirm the clean/private boundary.
- Decide whether public releases should commit/source-control the built jar or build it only for release packages.

## Leak Scan Terms

Before public push/package, scan the public output for:

```text
AGENTS.md
.agent/
.agent\
HANDOVER
PLANS
LESSONS
Codex
D:\Sean Mods
C:\Games\Starsector
starfarer_obf
CargoStackView
bytecode
patched badge
patched cargo-cell
WeaponsProcurementBadgeHelper
WeaponsProcurementCountUpdater
wp_enable_patched_badges
wp.config.patchedBadgesEnabled
wp.private.patchedBadgesEnabled
wp_total_
tools/patcher
validate-cargo-stack-view-patch
validate-total-badges
generate-total-badges
```

Some terms such as `agent` may appear inside ordinary words; review matches rather than treating every hit as a failure.

## Suggested Public Validation

Public-output validation should eventually include:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
git diff --check
```

Private-only validation may still run badge/patcher checks in the private repo.

## Version And Changelog Rules

- Do not bump version for private archive/doc restructuring.
- Bump patch version for public release cleanup, packaging validation fixes, or public-safe config corrections.
- Bump minor version for new user-facing procurement features.
- Keep `mod_info.json` and `CHANGELOG.md` aligned when bumping.
- Public changelog entries must not mention Codex, agents, private docs, local paths, or badge/bytecode experiments.

## Open Work Before First Public Release

- Build the exported public tree and validate it independently.
- Add a public package command if the public repo should ship `jars/weapons-procurement.jar`.
- Decide whether public repo ships the built jar or source-only plus build instructions.
