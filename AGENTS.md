# Weapons Procurement Agent Guide

Repo-local instructions for Weapons Procurement. These override the workspace-level `D:\Sean Mods\AGENTS.md` where more specific.

## Project Root

- Repo: `D:\Sean Mods\Weapons Procurement`
- Game: Starsector `0.98a`
- Live clean-package target: `C:\Games\Starsector\mods\Weapons Procurement`
- Runtime jar: `jars/weapons-procurement.jar`
- Main plugin: `weaponsprocurement.plugins.WeaponsProcurementModPlugin`
- Required dependencies: LazyLib and LunaLib

## Standard Commands

Start all repo-changing work with:

```powershell
git status --short --branch
```

Docs-only checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1 -IncludePrivateDocs
git diff --check
```

Runtime/source validation:

```powershell
$env:STARSECTOR_DIRECTORY = "X:\Path\To\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1 -IncludePrivateDocs
git diff --check
```

Private patched-badge validation:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-total-badges.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-cargo-stack-view-patch.ps1
```

Public export check:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\export-public.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
```

## Deploy Policy

- Use `tools/deploy-live-mod.ps1`; do not hand-copy runtime files unless explicitly asked.
- Clean deploys reject jars containing optional patched-badge classes by default. Use `-AllowPrivateBadgeJar` only for an intentional private patched-badge deploy.
- Deploy runtime changes that affect jar code, `mod_info.json`, `data/`, `graphics/`, Luna settings, generated assets, or package metadata.
- Do not deploy docs-only changes unless the user asks or release packaging requires mirrored docs.
- If deployment is blocked by a running Starsector process or locked artifact, do not kill the process. The deploy script stages the built files and queues a waiting background deploy.
- Validate the live artifact with `tools/validate-live-gui-classes.ps1` after runtime deploys.

## Public Release Policy

- Private repo work happens here; public output for `Shattersphere-Mods` must be curated, not mirrored.
- Read `.agent/PUBLIC_RELEASE.md` before any public export, public repo sync, package prep, or release-facing docs change.
- Public output must exclude `AGENTS.md`, `.agent/`, `HANDOVER.md`, `PLANS.md`, private archives, local paths, deploy queues, and optional patched-badge/bytecode material unless explicitly approved.
- Public changelog entries must stay user-facing and must not mention agents, private docs, local paths, or private experiments.

## Durable Docs

- `.agent/INDEX.md`: map of active docs and archives. Read this before loading large project docs.
- `.agent/BRIEF.md`: compact current state and next-step handoff.
- `.agent/PUBLIC_RELEASE.md`: private checklist for curating public repo/package output. Never publish it.
- `HANDOVER.md`: stable architecture, commands, and current validation constraints.
- `PLANS.md`: active plan only.
- `.agent/archive/INDEX.md`: archive map. Read this before opening deep dives.

Do not read every archive file at session start. Search first, then open only the relevant deep dive.

## Project Knowledge Map

- Starsector UI/classloader and row-layout pitfalls: `.agent/archive/deep-dives/starsector-ui.md`.
  Read before campaign UI helper extraction, stock-review layout work, button/poller changes, or row-width/indent fixes.
- Vanilla weapon hover tooltip bytecode: `.agent/archive/deep-dives/vanilla-weapon-tooltip-bytecode.md`.
  Read before trying to match or reuse vanilla cargo/refit weapon hover tooltips.
- Trade planning, source modes, quote semantics, and transaction side effects: `.agent/archive/deep-dives/trade-and-sources.md`.
  Read before changing Local/Sector/Fixer behavior, pending trades, tariffs, source allocation, or cargo mutation.
- Optional patched cargo-cell badges and bytecode constraints: `.agent/archive/deep-dives/patched-badges.md`.
  Read before patcher, badge helper, count bridge, bytecode inspection/injection, or cargo-cell badge work.
- Runtime and release validation procedures: `.agent/archive/deep-dives/runtime-validation.md`.
  Read before release validation, manual in-game testing, rollback fault tests, or live deploy troubleshooting.
- Public release/export boundary: `.agent/PUBLIC_RELEASE.md`.
  Read before syncing to `Shattersphere-Mods`, preparing a public package, or removing private/badge-only traces from release output.

## Hard Constraints

- Clean `F8` popup is the public/default product. Optional patched badges must remain isolated.
- Clean builds use `src/main/kotlin` plus legacy public Java under `src/weaponsprocurement`; private badge builds use `src/privateBadge`.
- Do not ship or commit a prepatched `starfarer_obf.jar`.
- Do not call Starsector campaign APIs from embedded patched-core badge helpers.
- Do not reintroduce visible seller-detail/source-specific local buy rows without a design pass.
- Treat compile success and jar parity as insufficient proof for runtime UI, LunaLib, or campaign behavior.
- Keep dangerous validation hooks disabled by default.
