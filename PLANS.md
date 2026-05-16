# Weapons Procurement Plans

## Current Status

The repo is in release-prep shape for the clean popup path:

- `F8` stock-review popup works from market/storage dialogs.
- Optional dialog entry exists and defaults off.
- Weapons and fighter LPCs share the same stock/review/trade flow.
- Local, Sector Market, and Fixer's Market source modes exist.
- Sector Market drains real remote cargo; Fixer's Market is virtual.
- Fixer's Market learns safe observed stock over time; the preferred next catalog model is runtime theoretical sale capability plus rarity estimates, with observation used as correction.
- Optional patched cargo-cell badges are private-only and isolated from the clean popup.
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

### Fixer's Market Theoretical Catalog

Replace the observation-first Fixer catalog with a runtime sale-capability index for weapons and fighter LPCs.

Steps:

1. Add an `ObservedStockIndex` that indexes exact current market cargo from `MarketAPI -> SubmarketAPI -> CargoAPI`.
2. Add a `TheoreticalSaleIndex` that derives vanilla-supported candidate weapons and fighter LPCs from runtime faction known sets, submarket type, item tags/hints, tier caps, and existing remote-market blacklist rules.
3. Add a `RarityClassifier` that uses tier, sell frequency, faction knowledge, market/submarket type, and observed corrections to explain likelihood without forcing market restocks.
4. Keep the existing observed Fixer catalog as a correction layer and fallback evidence source.
5. Treat custom submarkets as unknown for theoretical capability until an explicit adapter exists.

Acceptance:

- New games can show plausible Fixer weapons and fighter LPCs before those items have appeared in observed live cargo.
- Current live stock remains exact and source-draining behavior is unchanged for Sector Market.
- Fixer's Market still excludes no-sell/system/spoiler/private items and respects blacklists.
- No code calls submarket restock/update methods to probe availability.

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
