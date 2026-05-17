# Trade and sources deep dive

Status: active-reference
Scope: Weapons Procurement trade planning, source modes, quotes, cargo mutation, and transaction reporting
Last verified: 2026-05-17, against local Starsector 0.98a bytecode and installed Diable Avionics 2.9.4 data
Read when: changing Local/Sector/Fixer behavior, pending trades, quotes, tariffs, market blacklists, cargo mutation, or transaction callbacks
Do not read for: pure UI row layout, patched badge rendering, or docs-only release notes
Related files: `src/weaponsprocurement/core`, `src/weaponsprocurement/gui/StockReviewTrade*`, `src/weaponsprocurement/gui/StockReviewExecutionController.java`, `data/config/weapons_procurement_market_blacklist.json`
Search tags: `Sector Market`, `Fixer's Market`, `StockPurchaseExecutor`, `StockReviewQuoteBook`, `reportPlayerMarketTransaction`, `TheoreticalSaleIndex`, `ObservedStockIndex`, `RarityClassifier`, `FactionAPI`, `W:`, `F:`

## Summary

- Stock items are typed with `W:` and `F:` keys; raw ids are compatibility inputs only.
- Local, Sector Market, and Fixer's Market have intentionally different source and drain semantics.
- Sector Market stock is live and should not be cached across popup rebuilds.
- Fixer's Market uses a runtime theoretical-sale catalog plus rarity estimate, with observed stock used for reference prices and correction.
- Pending trades are signed staged intent; only `Confirm Trades` mutates cargo/credits.
- Quote the whole portfolio when planned lines can contend for the same stock.
- Use the final source-stock preflight and rollback journal around WP-touched cargo and credits.
- Treat transaction callbacks as post-commit side effects.
- Do not probe availability by forcing submarket updates; that mutates live market cargo and still gives weak statistics.

## Index

- `Stock Items And Row Scope`: item key and row inclusion rules.
- `Source Modes`: Local/Sector/Fixer source semantics.
- `Fixer's Market Catalog Direction`: theoretical catalog, rarity, and observed correction model.
- `Vanilla Market Sale Model`: source-grounded rules for current and theoretical market stock.
- `Diable Verification Notes`: installed-mod data that confirms why prefix/tag scanning is insufficient.
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
- `Fixer's Market` offers virtual 999-stock, applies `wp_fixers_market_price_multiplier`, and does not drain real market cargo.
- The current implementation builds Fixer's Market from live observed references, persistent observed fallback data, and deterministic runtime theoretical capability for weapons/fighter LPCs.
- Do not cache Sector Market stock across popup snapshot rebuilds. It represents live remote cargo that purchases can drain.
- Remote source modes disable black-market selling. Selling in remote modes uses the current local legal buyer.
- Sector and Fixer's Market can be independently disabled through LunaLib.
- `data/config/weapons_procurement_market_blacklist.json` blocks item keys, raw ids, weapon display names, and wing display names from remote source rows.

## Fixer's Market Catalog Direction

Fixer's Market exists to bypass market RNG for a markup, so a cold-start blank catalog is not ideal. The best current design is:

1. `ObservedStockIndex`: exact current stock from live cargo.
2. `TheoreticalSaleIndex`: deterministic candidate set from runtime faction knowledge and vanilla-supported submarket filters.
3. `RarityClassifier`: explanation/ranking layer that estimates common/uncommon/rare/very rare/never without simulating stock rolls.
4. `ObservationCorrectionLayer`: quiet learning from real market visits that can adjust the static estimate over time.

The observed Fixer catalog remains valuable, but should become a correction/confidence signal rather than the only way an item enters the Fixer catalog. This avoids the current cold-start weakness where a new save may not show a weapon or fighter LPC until it has appeared in generated cargo.

Implementation guidance:

- Do not parse mod folders at runtime. Active mods are already merged into Starsector registries, and mods can patch faction files, mutate known lists, generate markets dynamically, or change ownership through Nexerelin-style systems.
- Build candidate stock after the campaign economy is loaded using `Global.getSector().getEconomy().getMarketsCopy()`, `MarketAPI`, `SubmarketAPI`, `CargoAPI`, `FactionAPI`, and `Global.getSettings()`.
- Treat custom submarkets as `UNKNOWN_CUSTOM_SUBMARKET` for theoretical capability unless an adapter exists; current cargo inspection is still exact.
- For current stock, inspect `SubmarketAPI.getCargo()` and `cargo.getMothballedShips()`. This is the only exact answer to "what is sold right now?"
- For theoretical vanilla-style weapons/fighter LPCs, start with the relevant faction's sell-frequency map when present; fall back to known items only when the sell-frequency map is absent or empty. Then apply tags, system-weapon checks, tier caps, zero/negative frequency exclusion, and blacklist/safety filters.
- For ships, keep capability and rarity separate. Faction-known hulls are candidate inputs, but `FleetFactoryV3`, priority hulls, hull frequency, doctrine, role/variant availability, combat budget, stability, and `maxShipSize` all affect whether a ship is plausible in a particular submarket.
- Do not call `updateCargoPrePlayerInteraction()` repeatedly to sample availability. Vanilla stock updates clear and re-roll ships/weapons, are gated by a 30-day interval, and use unseeded item RNG. Forced probing would mutate visible market state, hitch on load, and still misclassify rare items.

Current WP code state:

- `WeaponsProcurementFixerCatalogUpdater` scans real market cargo about once per in-game day and stores safe observed weapon/wing item keys in sector persistent data.
- `ObservedStockIndex` scans current real market cargo and supplies the cheapest live reference price/cargo-space data.
- `TheoreticalSaleIndex` uses runtime faction sell-frequency maps when present, otherwise faction known weapons/fighters, plus vanilla-supported submarket tier caps, item safety filters, and the Fixer blacklist to build cold-start virtual stock.
- `RarityClassifier` attaches common/uncommon/rare/very rare/unknown labels for Fixer records without changing prices; tier-0 candidates remain common unless filtered out.
- The theoretical weapon/LPC catalog uses the market-owner faction for vanilla open, military, and black markets. Live observed cargo still captures custom submarket behavior without broad independent-catalog leakage.
- Ship cataloging remains future work.
- Keep the persistent observed catalog as simple Java collections to avoid custom save-object compatibility problems.
- Sanitize persistent maps into string key/value entries before use.
- Exclude obvious special/hidden tags such as `restricted`, `no_dealer`, `omega`, `dweller`, `threat`, and codex-hidden/unlockable markers when offering virtual Fixer stock.

## Vanilla Market Sale Model

Verified from local Starsector 0.98a API bytecode:

- Open, military, and black-market stock generation uses faction knowledge as candidate input. Weapon picks iterate `FactionAPI.getKnownWeapons()`, fighter picks iterate `FactionAPI.getKnownFighters()`, ship generation goes through `FleetFactoryV3`, and hullmod picks use `FactionAPI.getKnownHullMods()`.
- Current stock is live cargo. Use `MarketAPI -> SubmarketAPI -> CargoAPI` for weapons/fighter LPCs and `cargo.getMothballedShips()` for ships.
- Open market weapon/fighter calls pass tier cap `0`.
- Military market weapon/fighter calls pass tier cap `3`.
- Black market weapon/fighter calls pass tier cap `3`.
- Hullmods use tier cap `1` for open markets and `4` for military/black markets.
- Black-market hullmods are sourced from the submarket faction through the no-faction overload, not from an explicit market-owner faction parameter.
- Weapon generation rejects `AIHints.SYSTEM` and the `no_sell` tag. The API also exposes `WEAPON_NO_SELL`, `WING_NO_SELL`, and `MILITARY_MARKET_ONLY`; scanners should reject those defensively for mods even though the checked vanilla bytecode paths mainly expose `no_sell` directly.
- Stock updates are gated by `minSWUpdateInterval = 30` days and `sinceSWUpdate`.
- Each stock update clears `cargo.getMothballedShips()` before re-rolling ships.
- `itemGenRandom` is constructed with `new Random()` in `BaseSubmarketPlugin`, so stock rolls are not deterministic per save.

Practical theoretical rules for WP/Fixer:

```text
Observed stock:
market/submarket live cargo
= exact current stock

Theoretical weapon/LPC capability:
market/submarket relevant faction
+ faction known weapon/wing set
+ submarket type
+ item tier/tags/hints
= vanilla-supported candidate set

Rarity:
candidate set
+ tier/frequency/role/doctrine/budget/stability/submarket
= player-facing estimate, not a hard sale gate
```

For weapons and fighter LPCs, static theoretical capability is strong enough to drive a Fixer catalog. For ships it is useful but less exact; mark low-confidence or custom-submarket cases clearly instead of pretending the classifier is exact.

## Diable Verification Notes

Installed local check used `C:\Games\Starsector\mods\Diable Avionics-2.9.4` as an example mod. It confirms the general model and the main traps:

- `diableavionics.faction` declares faction id `diableavionics`.
- The faction file declares 29 known ships and the same 29 as priority ships.
- Only 26 hulls in `ship_data.csv` have the `diable_ship_bp` tag. The explicit-only known hulls are `diableavionics_versant`, `diableavionics_maelstrom`, and `diableavionics_pandemonium`.
- The faction file declares 32 known weapons. All 32 are tagged `diable_weapon_bp`, and none of the known set had `no_sell` in the local data. `weapon_data.csv` contains a duplicate row for `diableavionics_uhlan`, so count unique ids rather than CSV rows.
- Unique known weapon tiers are: tier 0 = 9, tier 1 = 12, tier 2 = 8, tier 3 = 3.
- The faction file declares 12 known fighter wings. Known wing tiers are: tier 0 = 2, tier 1 = 3, tier 2 = 3, tier 3 = 4. None of the known wing set had `no_sell` in the local data.
- The Diable econ files define Diable-owned markets (`diableavionics_prison`, `diableavionics_ressource`, `diableavionics_eclipse`, `diableavionics_shadow`) with faction, size, conditions, and industries. They do not list individual weapons, fighter LPCs, ships, or explicit submarkets.
- `pirates.faction` also references Diable pirate hulls: `diableavionics_stratus_p`, `diableavionics_rime_p`, `diable_tidalp`, and `diableavionics_chinookp`.

Consequences:

- Do not infer sale capability from item id prefixes such as `diableavionics_*`.
- Do not infer faction knowledge by scanning blueprint tags alone; explicit faction lists matter.
- Do not treat `data/factions/weapon_categories.csv` or `data/factions/fighter_wings.csv` as sale catalogs. They are auxiliary weighting/autofit data and can omit known items or include no-sell/non-known items.
- Use runtime `FactionAPI.getKnownWeapons()`, `getKnownFighters()`, `getKnownShips()`, and `getKnownHullMods()` when possible.

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
