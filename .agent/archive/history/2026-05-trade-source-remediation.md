# Trade and source remediation history

Status: historical
Scope: Weapons Procurement source modes, trade planning, execution hardening, and review-agent remediation
Last verified: 2026-05-12, summarized from the pre-debloat `PLANS.md`
Read when: investigating why trade/source code has its current safety boundaries or why a prior review task is already considered complete
Do not read for: ordinary row layout work or patched badge rendering
Related files: `src/weaponsprocurement/core`, `src/weaponsprocurement/gui/StockReviewTrade*`, `src/weaponsprocurement/gui/StockReviewExecutionController.java`, `data/config`
Search tags: `rollback`, `Sector Market`, `Fixer's Market`, `perItem`, `BUY_FROM_SUBMARKET`, `StockPurchaseExecutor`, `StockReviewQuoteBook`, `TradeMoney`

## Summary

- Local/Sector/Fixer source modes replaced the earlier Local/Global boolean model.
- Sector Market became a live remote-stock source that drains real market cargo.
- Fixer's Market became a virtual source backed by observed safe market stock and optional gated inference.
- Trade planning moved to signed pending trades, whole-portfolio quotes, cheapest-source allocation, and a Review/Confirm flow.
- Execution gained source-stock preflight, mutation journaling, rollback diagnostics, long-money arithmetic, and post-commit transaction reporting.
- Several review-agent findings were completed and should not be reopened without new evidence.
- Remaining trade/source proof is runtime rollback fault validation in game.

## Index

- `Source mode evolution`: Local, Sector Market, and Fixer's Market.
- `Trade planning evolution`: signed plans, review flow, quote ownership.
- `Execution hardening`: rollback, callbacks, money arithmetic, failure handling.
- `Review remediation completed`: source/config/CI/doc cleanup tasks already handled.
- `Still runtime-gated`: work that still needs in-game proof.

## Details

## Source mode evolution

The old source model was replaced with explicit modes:

- `Local`: current market review with Black Market toggle.
- `Sector Market`: live sector-wide stock with real market/submarket identity, multiplier pricing, and actual remote cargo draining.
- `Fixer's Market`: virtual stock using observed safe items, optional inference, multiplier pricing, and no real cargo draining.

The old plan also recorded these completed decisions:

- keep Sector Market stock uncached across rebuilds;
- independently gate Sector/Fixer availability through LunaLib;
- use a JSON blacklist for Sector/Fixer exclusions;
- default Fixer's tag/faction inference off because it risks secret/restricted weapons;
- disable and gray out the Black Market button for non-local source modes;
- add `Tariffs Paid` / average-markup summary.

## Trade planning evolution

The trade flow evolved from direct row purchases into staged signed plans:

- positive plan quantity means buy;
- negative plan quantity means sell from player inventory;
- `Review Trades` opens a confirmation screen before mutation;
- `Purchase All Until Sufficient` queues cheapest-first buys to desired stock;
- `Sell All Until Sufficient` queues inventory sales that keep post-trade stock sufficient;
- reset actions clear plans without mutating cargo;
- row adjustments unwind opposite queued plans before adding new buys/sells.

Quote ownership moved into:

- `StockReviewTradeContext` for render/controller affordability state;
- `StockReviewQuoteBook` for sorted sellers, line quotes, allocations, sell prices, and cargo estimates;
- `StockReviewPortfolioQuote` for whole-plan pricing when lines contend for the same seller stock;
- cached indexes in `WeaponStockSnapshot` to avoid repeated list reconstruction and linear scans.

## Execution hardening

Completed hardening included:

- local buy/sell transaction callbacks through `PlayerMarketTransaction`;
- `StockPurchaseChecks` for validation and safe messages;
- `StockItemCargo` for stack/count/add/remove/reconcile/tidy/display-name helpers;
- `StockItemStacks` for visibility, item id, price, legality, and cargo-space policy;
- `StockMarketTransactionReporter` for best-effort callback construction;
- `StockPurchaseMarketSources` for local/sector source discovery and sell-target choice;
- `StockPurchasePlan` for cheapest-source plan math;
- `StockPurchaseExecutor` for mutation-phase execution;
- final source-stock preflight before mutating source cargo;
- mutation journal/rollback for WP-touched counts and credits;
- post-commit transaction reporting so rollback paths do not fire non-rollbackable side effects first;
- `TradeMoney`/long totals so large modded prices fail closed instead of overflowing `int`.

## Review remediation completed

The old plan recorded these review-agent items as completed:

- no-op patched-badge count updater unless badges are enabled;
- committed-whitespace CI check;
- repo-jar stale-class CI check;
- remote-source tooltip correction;
- `perItem` config with legacy `perWeapon` compatibility;
- removal of stale `BUY_FROM_SUBMARKET`;
- rollback diagnostics and forced-failure hook;
- Java fallback for patched badges defaults off;
- wing-capable UI wording;
- wing display-name blacklist matching;
- repository-relative README `PACKAGING.md` link;
- decoupled Luna settings refresh from `StockReviewConfig.load()`;
- sanitized Fixer observed-catalog persistent data;
- non-proprietary CI sanity checks;
- clean/sync live deploy script;
- config documentation;
- Luna source descriptions mentioning fighter LPCs;
- version/changelog release prep.

## Still runtime-gated

The old plan preserved one unfinished proof:

- runtime-validate rollback after source removal, player cargo add/remove, target cargo add, and credit mutation for local, Sector Market, Fixer's Market, and mixed sell-then-buy plans.

This remains in active `PLANS.md`.

## Evidence / Provenance

- Summarized from deleted completed sections of `PLANS.md` before commit `a0e647b` (`Debloat repo documentation`).
- Current trade/source behavior is in `HANDOVER.md`; detailed trade rules are in `.agent/archive/deep-dives/trade-and-sources.md`.
