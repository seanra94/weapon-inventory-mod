# Archive Index

Archives preserve useful detail that should remain searchable but should not be startup context. Search this file first, then open only the relevant deep dive.

## Deep Dives

| Path | Category | Status | Read when | Key tags |
| --- | --- | --- | --- | --- |
| `.agent/archive/deep-dives/starsector-ui.md` | Starsector UI / classloader | active-reference | Changing stock-review layout, row sizing, `WimGui*`, buttons, scrolling, text fitting, helper extraction, or live-jar class validation. | `WimGui`, `buttonPressed`, `indent`, `a02e507`, `NoClassDefFoundError`, `validate-live-gui-classes` |
| `.agent/archive/deep-dives/vanilla-weapon-tooltip-bytecode.md` | Starsector UI / bytecode | active-reference | Attempting to match vanilla weapon hover tooltips, cargo-cell tooltip behavior, weapon row icons, or codex weapon tooltip layout. | `CargoDataGridView`, `CargoTooltipFactory`, `StandardTooltipV2`, `TooltipLocation.RIGHT`, `StockReviewWeaponIconPlugin`, `codex` |
| `.agent/archive/deep-dives/trade-and-sources.md` | Trade/source behavior | active-reference | Changing Local/Sector/Fixer sources, Fixer catalog inference, pending trades, quotes, tariffs, cargo mutation, transaction callbacks, or market blacklists. | `Sector Market`, `Fixer's Market`, `TheoreticalSaleIndex`, `ObservedStockIndex`, `RarityClassifier`, `FactionAPI`, `StockPurchaseExecutor`, `StockReviewQuoteBook`, `W:`, `F:` |
| `.agent/archive/deep-dives/patched-badges.md` | Optional patched badge path | active-reference | Changing patcher code, bytecode injection, badge helper, count bridge, badge assets, or `CargoStackView` validation. | `CargoStackView`, `starfarer_obf.jar`, `WeaponsProcurementBadgeHelper`, `wp.private.patchedBadgesEnabled`, `wp.counts.ready`, `javap`, `bytecode` |
| `.agent/archive/deep-dives/runtime-validation.md` | Runtime/release validation | active-reference | Preparing manual in-game validation, rollback fault checks, patched badge checks, release validation, or deploy troubleshooting. | `deploy-live-mod`, `queued deploy`, `rollback`, `wp.debug.failTradeStep`, `validate-cargo-stack-view-patch`, `validate-total-badges` |

## History

| Path | Category | Status | Read when | Key tags |
| --- | --- | --- | --- | --- |
| `.agent/archive/history/2026-05-gui-framework-migration.md` | GUI migration history | historical | Investigating why the popup uses the current `WimGui*` custom-panel/list architecture or why a helper/validator exists. | `WimGui`, `ACG`, `StockReviewRenderer`, `NoClassDefFoundError`, `modal list` |
| `.agent/archive/history/2026-05-trade-source-remediation.md` | Trade/source remediation history | historical | Investigating completed review-agent tasks, source-mode evolution, rollback hardening, or trade execution boundaries. | `rollback`, `Sector Market`, `Fixer's Market`, `perItem`, `BUY_FROM_SUBMARKET`, `TradeMoney` |
| `.agent/archive/history/2026-05-product-and-validation-history.md` | Product/release history | historical | Investigating clean-vs-patched product boundary, release validation posture, or why badge approaches are avoided. | `clean popup`, `patched badges`, `CargoStackView`, `late over-icon`, `Sanity`, `deploy-live-mod` |

## Retired Plans

No retired plan files yet. Keep `PLANS.md` active-only; move old roadmap material here only when preserving it has clear value.
