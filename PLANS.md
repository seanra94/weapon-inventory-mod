# Weapons Procurement Plans

## Current Status

The repo is in release-prep shape for the clean popup path:

- `F8` stock-review popup works from market/storage dialogs.
- Optional dialog entry exists and defaults off.
- Weapons and fighter LPCs share the same stock/review/trade flow.
- Local, Sector Market, and Fixer's Market source modes exist.
- Sector Market drains real remote cargo; Fixer's Market is virtual.
- Fixer's Market uses runtime theoretical sale capability plus rarity estimates, with observed live/persistent stock used for reference prices and correction.
- Optional patched cargo-cell badges are private-only and isolated from the clean popup.
- Gradle/Kotlin migration foundation is underway; `build.ps1` remains the compatibility entry point.
- Packaging, deploy, live-jar validation, doc-link validation, and GitHub sanity checks exist.

## Active Work

### Public Release Separation

Prepare the private repo so a curated public `Shattersphere-Mods` repo/package can contain the clean procurement GUI with no badge/bytecode/private-doc traces.

Completed:

- Badge-only Java files can be omitted while the clean GUI still builds.
- The clean plugin no longer imports or directly registers badge updater classes.
- Patched-badge Luna/settings publication moved out of the public-safe config path.
- Public validation is separated from private patched-badge validation.
- `tools/export-public.ps1` creates a curated public tree and runs a leak scan.
- Public README/PACKAGING docs describe only the clean GUI path.
- `tools/deploy-live-mod.ps1` stages and queues deploys when the live jar is locked.

Acceptance:

- Public output builds the clean GUI.
- Public output contains no `AGENTS.md`, `.agent/`, `HANDOVER.md`, `PLANS.md`, private archives, local machine paths, badge sprites, patcher tools, bytecode patch docs, or patched-badge settings.
- Private repo still retains the optional patched-badge work for personal/advanced use.

Still open:

- Decide whether the public repo should commit the built jar or build jars only for release packages.

### Kotlin Migration

Migrate the repo to Kotlin and a Gradle build in reviewable chunks while preserving the current public mod identity, jar path, config ids, and trade behavior.

Completed:

- Gradle/Kotlin wrapper foundation added while legacy Java still compiles.
- `build.ps1` delegates to Gradle for compatibility with existing commands.
- LazyLib is declared as a required dependency for the Kotlin runtime.
- Optional badge source is physically separated under `src/privateBadge`.
- `tools/validate-kotlin-migration.ps1` validates build files, dependency declarations, Java migration status, clean jar badge-class exclusion, and public-export boundaries.
- Core enums/value helpers and low-risk stock/Fixer catalog helpers converted to Kotlin while preserving the `weaponsprocurement.core` Java interop surface.
- Inventory counts, market stock collection, and market blacklist services converted to Kotlin.
- Global remote stock service and persistent Fixer observed catalog storage converted to Kotlin.
- Stock review config loading and stock snapshot assembly converted to Kotlin.
- Stock record/stat label helpers converted to Kotlin.
- Pending trade state, line quote, portfolio quote, and seller allocation value classes converted to Kotlin.
- Quote book and trade context converted to Kotlin.
- Trade validation, source lookup, and transaction-reporting helpers converted to Kotlin.
- Purchase service and executor mutation/rollback code converted to Kotlin.
- Shared WimGui and stock-review UI primitive/value classes converted to Kotlin.
- Stock-review source/filter/mode state and player-cargo helper code converted to Kotlin.

Still open:

- Convert UI/rendering/tooltips and split large GUI ownership.
- Convert lifecycle/plugin code and private badge source.
- Add final no-Java-source gate when conversion is complete.

### Runtime Rollback Validation

Validate the trade rollback fault hook in game.

Steps:

1. Open LunaLib settings.
2. Set `DEV ONLY: force trade rollback failure` to one failure step.
3. Test each relevant trade mode:
   - local legal buy;
   - local sell;
   - local black-market buy/sell if available;
   - Sector Market buy;
   - Fixer's Market buy;
   - mixed sell-then-buy plan.
4. Confirm player credits, player cargo, and touched market cargo return to pre-confirm values after forced failure.
5. Repeat for:
   - `after-source-removal`;
   - `after-player-cargo-remove`;
   - `after-player-cargo-add`;
   - `after-target-cargo-add`;
   - `after-credit-mutation`.
6. Reset the setting to `none`.

Acceptance:

- No GUI crash.
- Controlled failure message appears.
- WP-touched item counts and credits are restored.
- Successful trades still work after resetting the debug setting.

## Deferred Until Runtime Evidence

- Tune row widths only after seeing clipping at real UI scales or with long modded names.
- Add wing-specific filters only if wing rows are noisy in real saves.
- Improve failure messages for concrete observed cases such as no valid buyer, illegal market, access restriction, commission/faction restrictions, or modded submarket behavior.
- Add extra black-market suspicion/reputation/economy simulation only if runtime testing proves vanilla/modded transaction listeners are not firing through the current callback path.
- Narrow popup reopen state payload only if stale warning/scroll/mode state becomes a real bug.

## Avoid Unless Reopened Deliberately

- Do not retry late over-icon badge rendering; it caused invisibility/blur regressions.
- Do not use layered background plus text badge rendering for tiny cargo-cell badges.
- Do not put campaign-state or LunaLib calls into `WeaponsProcurementBadgeHelper`.
- Do not reintroduce runtime reflection or raw GL rendering for the badge path.
- Do not reintroduce seller-detail rows or source-specific local-buy actions without a deliberate UI design pass.
- Do not treat build/static validation as proof that custom Starsector UI behavior works in game.

## Validation Commands

For code or asset changes:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-total-badges.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-cargo-stack-view-patch.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

For docs-only changes:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```
