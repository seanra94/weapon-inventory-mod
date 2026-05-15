# Trade and sources deep dive

Status: active-reference
Scope: Weapons Procurement trade planning, source modes, quotes, cargo mutation, and transaction reporting
Last verified: 2026-05-12, archived from the pre-debloat `HANDOVER.md`
Read when: changing Local/Sector/Fixer behavior, pending trades, quotes, tariffs, market blacklists, cargo mutation, or transaction callbacks
Do not read for: pure UI row layout, patched badge rendering, or docs-only release notes
Related files: `src/weaponsprocurement/core`, `src/weaponsprocurement/gui/StockReviewTrade*`, `src/weaponsprocurement/gui/StockReviewExecutionController.java`, `data/config/weapons_procurement_market_blacklist.json`
Search tags: `Sector Market`, `Fixer's Market`, `StockPurchaseExecutor`, `StockReviewQuoteBook`, `reportPlayerMarketTransaction`, `W:`, `F:`

## Summary

- Stock items are typed with `W:` and `F:` keys; raw ids are compatibility inputs only.
- Local, Sector Market, and Fixer's Market have intentionally different source and drain semantics.
- Sector Market stock is live and should not be cached across popup rebuilds.
- Fixer's Market should prefer observed real market stock over spoiler-prone inference.
- Pending trades are signed staged intent; only `Confirm Trades` mutates cargo/credits.
- Quote the whole portfolio when planned lines can contend for the same stock.
- Use the final source-stock preflight and rollback journal around WP-touched cargo and credits.
- Treat transaction callbacks as post-commit side effects.

## Index

- `Stock Items And Row Scope`: item key and row inclusion rules.
- `Source Modes`: Local/Sector/Fixer source semantics.
- `Fixer's Market Safety`: observed catalog and inference cautions.
- `Sorting And Filtering`: source-aware sort/filter behavior.
- `Pending Trade Semantics`: queued trade rules.
- `Quote And Cost Ownership`: quote/cache/long-money ownership.
- `Execution Flow`: mutation ordering and transaction reporting.
- `Warning And Summary Rows`: footer warning and summary behavior.
- `Availability And Refresh`: opener gating and refresh behavior.

## Details

## Stock Items And Row Scope

- A stock item is either a weapon or a fighter LPC/wing.
- Shared runtime maps and pending trades use type-prefixed item keys:
  - `W:<weaponId>`
  - `F:<wingId>`
- The Make Trades screen should show only stock items that are buyable from the active source or present in player inventory and therefore sellable.
- Stored-only items should not create rows, though `Storage` may include accessible storage for an item that appears for another reason.
- Weapon filters apply only to weapons. Fighter LPC rows remain visible until wing-specific filters are deliberately added.
- Wing sufficiency defaults to 4 and is configurable through LunaLib.

## Source Modes

- `Local` reviews the current market and honors the Black Market toggle.
- `Sector Market` scans live in-sector market weapon and fighter LPC cargo, keeps real market/submarket identity on each stock source, applies `wp_sector_market_price_multiplier`, and drains actual remote market cargo on confirmation.
- `Fixer's Market` offers virtual 999-stock from safe observed items plus optional inferred faction-known weapons, applies `wp_fixers_market_price_multiplier`, and does not drain real market cargo.
- Do not cache Sector Market stock across popup snapshot rebuilds. It represents live remote cargo that purchases can drain.
- Remote source modes disable black-market selling. Selling in remote modes uses the current local legal buyer.
- Sector and Fixer's Market can be independently disabled through LunaLib.
- `data/config/weapons_procurement_market_blacklist.json` blocks item keys, raw ids, weapon display names, and wing display names from remote source rows.

## Fixer's Market Safety

- The observed Fixer catalog is the preferred public-build path because it learns only from real generated market stock.
- `WeaponsProcurementFixerCatalogUpdater` scans real market cargo on load and about once per in-game day, then stores safe observed weapon/wing item keys in sector persistent data.
- Keep the persistent catalog as simple Java collections to avoid custom save-object compatibility problems.
- Sanitize persistent maps into string key/value entries before use.
- Optional tag/faction inference is Luna-gated by `wp_enable_fixers_market_tag_inference` and defaults off.
- Inference should use explicit `FactionAPI.getWeaponSellFrequency()` entries first, fall back to `getKnownWeapons()` only when a faction has no sell-frequency data, and exclude obvious special/hidden tags such as `restricted`, `no_dealer`, `omega`, `dweller`, `threat`, and codex-hidden/unlockable markers.

## Sorting And Filtering

Sort modes:

- `Stock`: lowest visible `Storage` count first, then cheapest current buy price, then item name.
- `Name`: item name first, then need, then price.
- `Price`: cheapest current buy price first, then most needed, then item name.

Filter rules:

- The `Filters: N` button opens a scrollable filter screen.
- Active filters appear at the top and can be toggled off there.
- `Size`, `Type`, and `Damage` are expandable filter headings.
- Filters apply immediately to stock category counts and weapon rows.
- Filter logic is OR inside a filter group and AND across active groups.
- Fighter LPC rows remain visible until wing-specific filters are deliberately added.

## Pending Trade Semantics

- Positive pending trade quantities mean queued buys.
- Negative quantities mean queued sells.
- Zero is neutral and should not enter pending-trade state.
- `StockReviewPendingTrade.create(...)` should remain the construction gate; it rejects empty item keys and zero quantities.
- Pending-trade merge/reset/clear/executed-removal behavior belongs in `StockReviewPendingTrades`.
- `removeExecuted(...)` removes by queued trade value rather than object identity.
- Row-level buy/sell buttons first unwind the opposite queued plan before adding a real buy/sell.
- Sell buttons queue sells from player cargo only, not broader accessible storage.
- `Purchase All Until Sufficient` queues cheapest-first buys needed to reach desired stock while respecting existing plans, credits, cargo space, and seller stock.
- `Sell All Until Sufficient` queues inventory sales only when the post-trade stock level remains sufficient.
- `Sufficient` adjusts the item to barely sufficient status by buying a deficit or selling excess.
- `Sufficient` uses the current queued plan as part of its calculation.

## Quote And Cost Ownership

- Performance-sensitive trade math should go through `StockReviewTradeContext`.
- `StockReviewTradeContext` caches pending buy/sell quantities, per-item costs, total cost, cargo-space delta, current credits, cargo space, and affordability probes for the current render/controller action.
- Market quote/pricing work should go through `StockReviewQuoteBook`.
- `StockReviewQuoteBook` caches sorted buyable seller lists, line quotes, seller allocations, sell prices, and fallback cargo-space values.
- Whole-plan pricing should use `StockReviewPortfolioQuote`, not independent line quotes. Generic and source-specific buys can overlap the same market stock, so the whole planned portfolio must consume remaining seller stock once.
- Quote result data uses explicit top-level GUI classes such as `StockReviewQuote` and `StockReviewSellerAllocation`.
- `WeaponStockSnapshot` keeps cached all-record and item-key maps. Do not reintroduce repeated `getAllRecords()` reconstruction or linear id scans in render loops.
- Trade money totals use `long` helpers and must fail closed when an order is too large for safe Starsector credit mutation.

## Execution Flow

Execution order:

1. sells;
2. explicit source buys if that feature is deliberately reintroduced;
3. generic cheapest buys.

Rules:

- The current UI intentionally has no visible seller rows.
- Generic buy allocation is cheapest-first among all currently eligible sources.
- If black-market/source rules include black-market stock and it is cheaper than legal stock, generic `+1` may consume black-market stock first.
- Keep `StockReviewQuoteBook` preview ordering and `StockPurchaseService` execution ordering in sync.
- Fixer's Market reference pricing uses the cheapest live source as its base reference, regardless of whether that source is legal or black-market stock.
- `StockPurchaseExecutor.buyPlan(...)` must perform final all-line source-stock preflight immediately before mutating source cargo.
- `StockPurchaseExecutor` keeps a narrow mutation journal around WP-touched item counts and player credits.
- If execution throws after mutation begins, it should best-effort reconcile those counts/credits and log before/failure/rollback counts.
- Transaction callbacks should be post-commit side effects. Do not fire `SubmarketPlugin.reportPlayerMarketTransaction(...)` before rollbackable cargo and credit mutations have succeeded.
- Direct local-market cargo mutations are followed by best-effort `SubmarketPlugin.reportPlayerMarketTransaction(...)` with bought/sold cargo and line-item data.
- Runtime testing suggested expected black-market penalties fired through the callback path; do not add extra suspicion/reputation simulation unless testing proves the callback path is insufficient.
- Local sells deposit into the black market when the Black Market toggle is enabled; otherwise they use the best eligible non-black trade submarket.
- After a sell callback, WP reconciles the touched submarket cargo to the exact pre-sale count plus sold quantity because vanilla/modded callbacks can normalize stock.
- Fixer's Market buys are virtual WP transactions and intentionally do not report to a real submarket plugin.
- Sector Market buys remove stock from real remote market cargo and report best-effort transactions to the touched remote submarket.

## Warning And Summary Rows

- `Warning`, `Tariffs Paid`, `Credits Available`, and `Cargo Space Available` belong in fixed bottom rows above footer buttons, not in the scrollable item list.
- `Warning` starts at `None` and otherwise displays the most recently triggered trade warning.
- Warnings include:
  - `Not enough cargo capacity`;
  - `Not enough credits`;
  - `Credit balance at <5% of initial balance`;
  - `Cargo capacity at <5% of total capacity`.
- Plan-changing actions should refresh the warning back to `None` when no warning remains.
- `Tariffs Paid` is markup above true base item value, with an average multiplier label such as `[avg 4.0x]`.
- `Credits Available` and `Cargo Space Available` include current value plus signed bracketed delta.

## Availability And Refresh

- `F8` should be gated to `SectorAPI.getCurrentlyOpenMarket()` plus at least one buyable local item, one player-cargo item that can be sold, or an enabled remote source.
- It should not open from looting or non-trade planet contexts.
- The preferred buy path mutates cargo and rebuilds the popup snapshot in place.
- Forced close/reopen of the vanilla cargo core remains only as a fallback for stale vanilla trade-grid slot views.

## Evidence / Provenance

- Extracted and cleaned from the detailed `HANDOVER.md` that existed before commit `a0e647b` (`Debloat repo documentation`).
- Current active architecture and validation status remain in `HANDOVER.md` and `PLANS.md`.
- Keep this file as a reference for source/trade changes, not a substitute for runtime validation.
