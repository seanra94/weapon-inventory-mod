# Weapons Procurement Handover

## Purpose

Weapons Procurement is a Starsector `0.98a` mod for reviewing weapon and fighter LPC stock, planning buys/sells, and confirming trades from market or storage dialogs.

The primary public path is the clean popup:

- open a market/storage dialog;
- press `F8`, or enable the optional market-dialog entry in LunaLib;
- queue buys/sells;
- review and confirm trades.

The optional patched cargo-cell badge path remains isolated and advanced-use only. Do not ship a prepatched `starfarer_obf.jar`.

## Current Product Shape

- `mod_info.json` id: `weapons_procurement`.
- Required dependency: LunaLib.
- Runtime jar: `jars/weapons-procurement.jar`.
- Main plugin: `weaponsprocurement.plugins.WeaponsProcurementModPlugin`.
- Clean package files: `data/`, `graphics/`, `jars/`, `mod_info.json`, `README.md`, `CONFIG.md`, `CHANGELOG.md`, `PACKAGING.md`.

User-facing docs:

- `AGENTS.md`: repo-local rules, commands, deploy policy, and knowledge map.
- `README.md`: install/use summary.
- `CONFIG.md`: JSON item overrides, blacklists, Luna settings, debug hooks.
- `PACKAGING.md`: release validation, clean package, optional patched-badge instructions.
- `CHANGELOG.md`: release notes.
- `.agent/INDEX.md`: map of active docs and archival reference notes.
- `.agent/BRIEF.md`: compact current-state handoff.
- `.agent/PUBLIC_RELEASE.md`: private public-export checklist for `Shattersphere-Mods`. Never publish it.
- `.agent/archive/INDEX.md`: archive map for deep dives and historical references.

## Entry Points

- `WeaponsProcurementModPlugin`: registers transient scripts on game load.
- `StockReviewHotkeyScript`: opens the `F8` popup from valid campaign dialogs.
- `WP_OpenDialog` plus `data/campaign/rules.csv`: optional dialog action.
- `WeaponsProcurementCountUpdater`: publishes optional patched-badge count properties while paused. It no-ops unless patched badges are enabled.
- `WeaponsProcurementFixerCatalogUpdater`: observes safe real market stock over time for Fixer's Market.

## Core Model

Stock items are weapons or fighter LPCs. Generic item state uses typed keys:

- `W:<weaponId>`
- `F:<wingId>`

Use typed keys for shared maps and trade state. Raw ids are accepted only where a config path intentionally preserves backward compatibility.

Important ownership:

- `InventoryCountService`: player cargo plus accessible storage counts.
- `MarketStockService`: current-market stock collection and submarket filtering.
- `GlobalWeaponMarketService`: Sector Market and Fixer's Market stock construction.
- `FixerMarketObservedCatalog`: save-persistent observed Fixer catalog.
- `ObservedStockIndex`, `TheoreticalSaleIndex`, `RarityClassifier`: Fixer's Market current-stock reference, runtime sale-capability catalog, and rarity labels.
- `WeaponMarketBlacklist`: Sector/Fixer blacklist matching by key, raw id, or display name.
- `StockReviewConfig`: JSON stock defaults and per-item overrides.
- `DesiredStockService`: effective desired thresholds.

## Source Modes

- `Local`: current market, with the normal Black Market toggle.
- `Sector Market`: live sector-wide stock from real market cargo. Purchases drain remote cargo and use the sector multiplier.
- `Fixer's Market`: virtual stock from safe runtime faction catalogs plus observed market reference prices. Purchases do not drain real cargo and use the fixer multiplier.

Remote source modes disable black-market selling. Sells while a remote source is active use the current local legal buyer.

Keep Sector Market stock live, not cached across popup rebuilds. It represents actual cargo that may be drained.

## Trade Flow

Pending trades are staged in `StockReviewPendingTrades` / `StockReviewPendingTrade`.

Execution order:

1. sells;
2. explicit source buys, if any are ever reintroduced;
3. generic cheapest buys.

The current UI uses generic cheapest buys. Do not reintroduce seller-detail/source-specific rows unless the feature is deliberately reopened.

Execution ownership:

- `StockReviewTradeController`: planning mutations.
- `StockReviewTradeContext` / `StockReviewQuoteBook`: quote and affordability state.
- `StockReviewExecutionController`: confirm flow, pre-confirm checks, per-line execution handling.
- `StockPurchaseService`: high-level buy/sell orchestration.
- `StockPurchasePlan`: cheapest-source plan construction.
- `StockPurchaseExecutor`: cargo/credit mutation, rollback journal, post-commit transaction reporting.
- `StockMarketTransactionReporter`: best-effort `PlayerMarketTransaction` callback reporting.

Trade money totals use `long` via `TradeMoney`; fail closed if a plan is too large to mutate safely through Starsector credit APIs.

Transaction callbacks should be post-commit side effects. Do not fire `SubmarketPlugin.reportPlayerMarketTransaction(...)` before rollbackable cargo and credit mutations have succeeded.

## GUI Architecture

`StockReviewPanelPlugin` is lifecycle/context orchestration. Keep domain work in focused controllers/renderers:

- `StockReviewRenderer`: shell/header/body/footer composition.
- `StockReviewListModel`: main trade rows.
- `StockReviewReviewListModel`: review rows.
- `StockReviewItemInfoRows`: basic/advanced item data rows.
- `StockReviewTradeRowCells`: storage/price/plan/action cells.
- `StockReviewFooterRenderer`: mode-specific footers.
- `StockReviewModeController`: review/filter/color-debug modes.
- `StockReviewUiController`: source/sort/filter/expansion/reset/navigation actions.
- `StockReviewExecutionController`: confirm-trade execution.

Shared `WimGui*` helpers own modal panels, list rendering, row cells, buttons, scrolling, input, tooltips, and campaign dialog host behavior. Future screens should compose these helpers instead of copying stock-review renderer logic.

Starsector GUI runtime constraints:

- Nested custom-panel buttons are not reliable through `buttonPressed(...)` alone; keep the event-gated `WimGuiButtonPoller` fallback.
- Avoid generated helper/anonymous classes in classloader-sensitive GUI paths unless the live jar validator is updated and in-game entry points are tested.
- Runtime UI alignment has been fragile. Commit `a02e507` is the known-good reference for nested stock-review indentation and right-edge sizing.

## Optional Patched Badge Path

The patched badge path exists only for exact in-cell vanilla cargo badges.

Rules:

- Keep patched helpers lookup-only; normal mod code computes state.
- Do not call Starsector campaign APIs from `WeaponsProcurementBadgeHelper`.
- Patch only `CargoStackView.renderAtCenter(FFF)V`.
- Keep patching reversible and refusal-oriented.
- Validate with `tools/validate-cargo-stack-view-patch.ps1`.
- Treat bytecode injection as an advanced/private path unless explicitly approved for a release target.

Normal clean popup work must remain independent of this path.

## Build And Validation

Normal code/asset validation:

```powershell
$env:STARSECTOR_DIRECTORY = "X:\Path\To\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-total-badges.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-cargo-stack-view-patch.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

`deploy-live-mod.ps1` clean-syncs repo-managed files to `C:\Games\Starsector\mods\Weapons Procurement`. If the live jar is locked by Starsector, it stages the built files and starts a minimized visible queued worker that publishes after the lock clears.

For docs-only edits, `validate-doc-links.ps1` and `git diff --check` are usually sufficient.

## Runtime Validation Still Required

Static/build validation is not in-game proof. The remaining high-value runtime check is rollback fault validation:

- set LunaLib `DEV ONLY: force trade rollback failure` to each failure step;
- test local buy, local sell, Sector Market buy, Fixer's Market buy, and mixed sell-then-buy plans;
- confirm WP-touched cargo counts and player credits return to pre-confirm values;
- reset the setting to `none` before normal play or packaging.
