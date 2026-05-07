# Weapon Inventory Mod Plans

## Current Status

- Working architecture:
  - deterministic `CargoStackView.renderAtCenter(FFF)V` bytecode patch for in-cell badge rendering;
  - normal mod-side paused updater for counts;
  - JVM `System` property bridge between updater and embedded helper;
  - one precomposed badge sprite per weapon or fighter LPC stack.
- Clean frontend work has started:
  - `F8` opens a normal Weapon Stock Review popup from active market/storage interaction dialogs;
  - popup data comes from shared stock snapshot services, not the bytecode badge path;
  - weapon entries now show `Current`, `For Sale`, `Plan`, and `Cost`/`Profit` cells plus buy/sell controls.
  - popup has config-backed desired stock defaults, display mode, storage inclusion, black-market inclusion, and per-weapon override scaffolding.
  - popup defaults to `All Tracked` so the review starts from all enabled weapon specs instead of only owned/currently sold weapons.
  - popup has sort modes for need, name, for-sale count, and owned count.
  - popup row actions now support weapon expansion, nested Weapon Data/Sellers sections, cheapest-first `Buy 1`/`Buy 10`, and submarket-specific buys.
- Current visual baseline:
  - bottom-right placement;
  - stable pre-scale render frame;
  - no late over-icon rendering;
  - no layered text/background rendering.
- Current config baseline:
  - LunaLib dependency is present;
  - Luna settings control updater interval and whether optional patched cargo-cell badges are enabled.

## Completed Meaningful Work

- Proved exact in-cell badges require a patch to vanilla cargo-cell rendering; a pure normal-mod version would need a different UI surface.
- Replaced raw GL/external hook attempts with direct `com.fs.graphics.Sprite` rendering in patched `CargoStackView`.
- Moved count computation out of patched core and into normal mod-side campaign code.
- Added paused dynamic updates for player cargo and accessible storage.
- Added fighter/LPC support using cargo fighter counts.
- Collapsed diagnostics to one precomposed total badge.
- Reverted away from late over-icon/layered rendering after visual regressions.
- Reintroduced LunaLib only through normal mod-side update interval config.
- Refactored updater counting to scan cargo into maps once per tick instead of repeating storage scans for every id.
- Removed old three-square diagnostic assets/settings and the legacy no-op `CargoWeaponMarkerHook`.
- Rewrote handover/plans docs to describe the current architecture instead of old investigation states.
- Added first clean read-only stock review popup:
  - shared stock snapshot builder;
  - current-market sale counts;
  - fleet + current-market-storage owned counts;
  - desired-count classifier;
  - three toggle headings;
  - runtime buttons for display mode, current-market storage, and black-market inclusion.
- Added optional patched-badge feature flag:
  - clean popup is unaffected;
  - patched helper returns `null` when disabled, so no badge renders even if the core jar is patched.
- Earlier popup redraw layering was mitigated by dismissing/reopening the tooltip dialog; the current cleaner foundation supersedes that with in-place custom-panel content replacement.
- Fixed row action routing around explicit `StockReviewAction` button ids; current custom-panel buttons use a narrow polling fallback because `buttonPressed(...)` alone was not reliable for nested row controls.
- Changed stock categories to start collapsed and render as flat full-width heading rows, so `No stock`, `Insufficient stock`, and `Sufficient stock` are visually peer sections.
- Replaced the tooltip-row popup renderer with an explicit custom-panel/list foundation:
  - `StockReviewListModel` builds render-ready row descriptors from the snapshot and state;
  - `StockReviewRenderer` renders fixed-height custom row panels for category headings, weapon rows, nested sections, sellers, and scroll indicators;
  - `StockReviewPanelBoxPlugin` owns reusable row/container fill and border drawing;
  - `StockReviewPanelPlugin` now rebuilds one custom content panel in place for non-purchase actions instead of dismissing/reopening the dialog;
  - list scroll offset is stored in `StockReviewState` and can be changed by mouse wheel or clickable scroll indicators.
- Restored `StockReviewButtonBinding` as a narrow fallback registry after runtime showed nested custom-panel buttons did not reliably trigger from `buttonPressed(...)` alone. Buttons still carry explicit `StockReviewAction` ids.
- Ported the accepted ACG GUI palette into `StockReviewStyle` and applied it to the stock review popup:
  - top stock category headings use red/yellow/green fills with white text;
  - nested toggle headings use the dark-gray collapsible heading color;
  - Buy buttons use the ACG load/yellow fill with white text;
  - ordinary popup text uses white/default font, with gray reserved for disabled/locked controls.
- Filtered stock review category counts and weapon rows to currently buyable market weapons only.
- Gated `F8` popup opening to an active current market with weapon stock for sale, avoiding looting and non-trade planet contexts.
- Changed successful popup purchases to rebuild the popup snapshot in place by default instead of force-refreshing the vanilla cargo core UI. The forced refresh fallback remains behind `StockReviewStyle.REFRESH_VANILLA_CORE_AFTER_PURCHASE`.
- Added first purchase flow:
  - top-level buy buttons plan from the cheapest eligible current-market submarkets;
  - seller rows can buy from a specific submarket;
  - purchases check credits and cargo space before mutating cargo.
  - successful purchases rebuild the popup snapshot in place by default; the older close/reopen fallback remains behind `StockReviewStyle.REFRESH_VANILLA_CORE_AFTER_PURCHASE`.
- Added signed planned-trade flow:
  - positive planned quantity means buying;
  - negative planned quantity means selling from player inventory;
  - `Review Purchase` opens a confirmation screen before cargo mutation;
  - `Purchase Until Sufficient` currently queues needed buys and opens the review screen.

## Active Manual Validation

- Open market trade and verify:
  - no crash;
  - `F8` opens the Weapon Stock Review popup;
  - `Esc` and `Close` dismiss the popup;
  - category headings expand/collapse and preserve current state across refresh;
  - Mode cycles through `Owned or For Sale`, `Currently For Sale`, `Owned Only`, and `All Tracked`;
  - Sort cycles through `Need`, `Name`, `For Sale`, and `Owned`;
  - Storage and Black Market toggles update counts/categories;
  - Mode/Sort/toggle actions no longer leave old text layered under new text;
  - weapon rows expand/collapse;
  - expanded rows show Weapon Data and Sellers sections;
  - top-level `Buy 1`/`Buy 10` buys from cheapest eligible seller stock;
  - seller-specific `Buy 1`/`Buy 10` only buys from that submarket;
  - failed buys show a message instead of mutating cargo;
  - no-weapons and many-weapons markets remain responsive;
  - commodities remain vanilla;
  - weapon badges still render bottom-right;
  - fighter LPC badges still render bottom-right;
  - totals still aggregate player cargo plus all accessible storage;
  - buy/sell/storage moves update while paused;
  - no old diagnostic squares or static marker assets appear.

## High-Value Future Work

- Rework Buy GUI weapon entries into the next stock-control row system:
  - target collapsed weapon-entry structure:
    - `Weapon Name` on gray background;
    - `Storage: N` on gray background;
    - `Inventory: N` on gray background;
    - `Stocked: N` on gray background;
    - `Buying: N` on green background when net planned quantity is positive;
    - `Selling: N` on red background when net planned quantity is negative;
    - neutral `Buying: 0` on yellow background when no trade is planned;
    - `Cost: $N` on red background for net cost;
    - `Profit: $N` on green background for net profit;
    - `- N`, `- 10`, `- 1`, `+ 1`, `+ 10`, `+ N`, `Reset` as compact controls;
    - `Buy Until Sufficient` or `+ N` should use green semantics where it represents reaching sufficiency.
  - `Storage` means accessible storage count, separate from player fleet cargo.
  - `Inventory` means player fleet cargo count, because only inventory weapons can be sold through this GUI.
  - `Stocked` means current total stock before planned trades, usually storage plus inventory under the active owned-source policy.
  - `Buying`/`Selling`, `Cost`/`Profit`, `Storage`, `Inventory`, and `Stocked` must remain snapshot/planned-trade summaries; clicking row controls should not mutate cargo until the Review GUI is confirmed.
  - `- N` means sell down until the weapon is barely sufficient, never below sufficient stock if already sufficient.
  - `+ N` means buy up until the weapon is barely sufficient, never beyond desired sufficient stock.
  - `Reset` sets that weapon's planned trade quantity back to zero.
  - `- 1` and `- 10` must be disabled with gray text and disabled-button visuals when there is not enough inventory stock available to sell that increment.
  - `+ 1` and `+ 10` must be disabled with gray text and disabled-button visuals when the planned running total would exceed current credits or cargo capacity, or when current market stock is insufficient for that increment.
  - Buy affordability and cargo-space checks must use the full current planned-trade total, not each weapon in isolation.
  - Weapon-name toggle headings can be about 25% narrower than the current row-label area to make room for the richer control grid.
  - Import/adapt the ACG text helper for button/row label fitting: shared wrapping, truncation, and `...` suffix behavior should be centralized rather than hand-coded per row.
  - Investigate why WIM buttons still do not visually match ACG's dimmed-inner-rectangle behavior. Compare WIM `StockReviewRenderer.addButton(...)` with the relevant ACG button helper and preserve the result in WIM's shared button template.
- Rework bottom Buy GUI bulk controls:
  - rename `Purchase Until Sufficient` to `Purchase All Until Sufficient` if that better matches the final UX.
  - Add `Sell All Until Sufficient`.
  - Add `Reset All Trades`.
  - `Purchase All Until Sufficient` should:
    - gather all current-market weapons available for purchase;
    - order purchase candidates from cheapest to most expensive;
    - buy as many as possible of each cheapest candidate until money runs out, cargo space runs out, seller stock runs out, or that weapon reaches barely sufficient stock;
    - respect any existing planned buys/sells rather than overwriting them;
    - leave the user in the Buy GUI with updated planned quantities and cost/profit cells, not jump straight to Review GUI unless explicitly changed later.
  - `Sell All Until Sufficient` should:
    - plan as many inventory sells as possible without moving a weapon from sufficient to insufficient or no-stock status after planned trades;
    - respect existing planned buys/sells rather than overwriting them.
  - `Reset All Trades` should clear all planned buy/sell quantities back to zero.
- Remove or reduce capped runtime diagnostic logs once the latest cleanup is manually validated.
- Consider Luna settings for thresholds only if the implementation can stay precomposed and asset-backed without runtime tint/layering.
- Harden purchase side effects after runtime validation:
  - transaction reporting/suspicion behavior;
  - tariff parity with vanilla;
  - clearer failure text for commission/illegal-market cases.
- Add a small validation command that checks patched `CargoStackView` helper-call counts and renders a concise report, instead of relying on ad hoc `javap` inspection.
- Consider publishing packaging notes for forum users that clearly explain the core-jar patch/restore requirement.

## Avoid Unless Reopened Deliberately

- Do not retry late over-icon rendering casually; it caused invisibility/blur regressions.
- Do not use layered background plus text badge rendering.
- Do not put campaign-state or LunaLib calls into `WeaponInventoryBadgeHelper`.
- Do not reintroduce runtime reflection or raw GL rendering for the badge path.
