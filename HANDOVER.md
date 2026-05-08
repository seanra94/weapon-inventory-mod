# Weapons Procurement Handover

## Current State

- The mod renders ownership-count badges directly inside vanilla market/trade cargo cells for weapon stacks and fighter LPC stacks.
- Exact in-cell rendering is implemented by deterministically patching `com/fs/starfarer/campaign/ui/trade/CargoStackView.renderAtCenter(FFF)V` in `starfarer_obf.jar`.
- A clean normal-mod frontend is now being added as the primary product: `F8` opens a Weapon Stock Review popup from an active market/storage interaction dialog.
- The popup is independent of the `CargoStackView` patcher. The clean UI should keep working if the patcher is removed.
- If `wp_enable_dialog_option` is enabled in LunaLib, normal market dialogs also add `Open "Weapon Procurement"` below the vanilla trade option. This is an optional convenience; keep `F8` working regardless.
- `PACKAGING.md` separates the forum-safe clean popup package from the optional patched badge package. Do not ship a prepatched `starfarer_obf.jar`; the patcher should operate on the user's installed game copy.
- Terminology:
  - `Buy GUI` means the main `F8` stock-review screen.
  - `Review GUI` means the planned-trade review screen opened from the Buy GUI.
  - `stock item` means either a weapon or fighter LPC/wing in the clean popup. Runtime trade state uses type-prefixed item keys internally (`W:` / `F:`) so a weapon id and wing id cannot collide.
  - `main headings` / `top level headings` means `No Stock`, `Insufficient Stock`, and `Sufficient Stock`.
  - `weapon entries` means rows under those main headings, such as `Light Needler (-)` plus stock, plan, cost/profit, and buy/sell controls.
- Popup configuration lives in `data/config/weapons_procurement_stock.json`:
  - include/exclude accessible storage in current stock calculations;
  - include/exclude black market stock;
  - desired stock defaults by weapon size;
  - per-weapon desired/ignored overrides.
- LunaLib now owns the user-facing default sufficient-stock thresholds by weapon mount size:
  - `wp_desired_small_weapon_count`, default 16;
  - `wp_desired_medium_weapon_count`, default 8;
  - `wp_desired_large_weapon_count`, default 4.
  These override the JSON defaults, while per-weapon JSON `desired` overrides still take precedence for specific weapons.
- Popup redraw rule:
  - The clean popup now renders through an explicit custom-panel shell, not one long tooltip row pile.
  - `StockReviewListModel` builds Buy GUI rows and `StockReviewReviewListModel` builds Review GUI rows.
  - `StockReviewRenderer` owns the popup shell, header, action row, footer, mode-specific row selection, and stock-specific scroll rows/top gaps; `WimGuiListRow` / `WimGuiListRowRenderer`, `WimGuiModalListLayout`, `WimGuiScrollableListState`, and `WimGuiScroll` own shared row rendering, modal-list, offset preservation, and scroll-window math.
  - `StockReviewPanelPlugin` rebuilds one root content panel for state changes through `WimGuiContentPanel`.
  - State-changing category/weapon/section/mode/sort/filter actions should rebuild the custom content panel in place and preserve `StockReviewState`, including list scroll offset.
- Popup button rule:
  - Buttons use real Starsector buttons with blank built-in labels plus a reusable `WimGuiButtonBinding` / `WimGuiButtonPoller` registry as a polling fallback. In runtime, nested custom-panel controls did not reliably arrive through `buttonPressed(...)` alone.
  - The fallback is deliberately event-gated: only poll for a few frames after mouse down/up events. Do not return to scanning every button every frame unless runtime testing proves the gated fallback misses clicks.
  - Keep row/button actions as explicit `StockReviewAction` ids; do not return to inferred checkbox state as the source of truth.
  - Clickable rows/buttons now use blank Starsector button text with WP-rendered labels layered separately, so hover/base colors stay ACG-like while visible text remains white/gray under WP control.
- Popup scope:
  - The old display-mode button and config mode were removed. The Buy GUI should show only weapons that are buyable from the active stock source or present in player inventory and therefore sellable through the GUI.
  - The Buy GUI now has top-level `Weapons` and `Wings` toggle headings. Each section then renders the usual `No Stock`, `Insufficient Stock`, and `Sufficient Stock` headings. Weapon filters apply only to weapons; wings remain visible while weapon filters are active until wing-specific filters are deliberately added.
  - Wing sufficiency defaults to 4 and is configurable through LunaLib as `wp_desired_fighter_wing_count`.
  - Stored-only weapons should not create rows. The `Storage` cell may still show full owned stock, including accessible storage, for a weapon that appears because it is buyable or in player inventory.
  - Local stock collection, local buys, local sells, and sell-price quoting should all use `MarketStockService.isTradeSubmarket(...)` for storage/local-resource/black-market filtering. Do not copy that submarket filter into each caller.
- Stock source modes:
  - The Buy GUI now cycles `Source: Local`, `Source: Sector Market`, and `Source: Fixer's Market`.
  - `Local` behaves like the normal current-market review and honors the Black Market toggle.
  - `Sector Market` scans live in-sector market weapon cargo, keeps real market/submarket identity on each stock source, applies the Luna multiplier `wp_sector_market_price_multiplier` (default `3.0`) to buy prices, and drains the actual remote market cargo stacks on confirmation. Selling while in Sector Market mode still sells to the current local market.
  - Do not cache Sector Market stock across popup snapshot rebuilds. It represents live remote cargo and must refresh after Sector Market purchases drain actual market stacks.
  - `Fixer's Market` is the virtual 999-stock source. It includes live-scanned eligible weapons plus optional inferred faction-known weapons, applies the Luna multiplier `wp_fixers_market_price_multiplier` (default `5.0`) to buy prices, and does not drain real market cargo. Selling while in Fixer's Market mode still sells to the current local market.
  - Fixer's Market also uses a save-persistent observed catalog. `WeaponsProcurementFixerCatalogUpdater` scans real market cargo on load and about once per in-game day, records safe observed weapon/wing item keys in sector persistent data, and lets the Fixer source keep offering virtual stock for those previously observed legal items even after they are no longer present in current live stock.
  - The Black Market button is disabled and displayed Off for both non-local source modes; remote source eligibility is controlled by the source mode itself, not by the local Black Market toggle.
  - Generic buy allocation is intentionally cheapest-first among all currently eligible sources. If the Black Market toggle/source rules include black-market stock and it is cheaper than legal stock, generic `+1` should consume the black-market stock first. Keep `StockReviewQuoteBook` preview ordering and `StockPurchaseService` execution ordering in sync.
  - Fixer's Market reference pricing also uses the cheapest live source as its base reference, regardless of whether that source is legal or black-market stock.
  - Optional tag/faction inference is Luna-gated by `wp_enable_fixers_market_tag_inference`, default off. Keep it separate from the live-scan path so it can stay opt-in if it admits secret/restricted weapons. The inference path uses active market factions' explicit `FactionAPI.getWeaponSellFrequency()` entries first, falls back to `getKnownWeapons()` only when a faction has no sell-frequency data, and excludes obvious special tags such as `restricted`, `no_dealer`, `omega`, `dweller`, `threat`, and codex-hidden/unlockable markers.
  - The observed Fixer catalog is the preferred public-build path over tag/faction inference: it may be incomplete early in a save, but it only learns from real generated market stock and therefore has much lower spoiler-leak risk.
  - Sector and Fixer's Market can be independently disabled in LunaLib. `data/config/weapons_procurement_market_blacklist.json` blocks weapon ids/display names from `BANNED_FROM_SECTOR_MARKET` and `BANNED_FROM_FIXERS_MARKET` before those remote source rows are created.
- Popup sorting:
  - `Stock`: lowest visible `Storage` count first, then cheapest current buy price, then weapon name;
  - `Name`: weapon name first, then need, then price;
  - `Price`: cheapest current buy price first, then most needed, then weapon name.
- Popup filters:
  - the top-row `Filters: N` button opens a scrollable filter screen built from the same modal-list row components as the stock/review/debug screens;
  - active filters are shown at the top and can be toggled off there;
  - `Size`, `Type`, and `Damage` are expandable filter headings;
  - filters apply immediately to stock category counts and weapon rows, with OR logic inside a filter group and AND logic across active groups.
- Popup purchase flow:
  - weapon entries in the Buy GUI use a signed `Plan`: positive values mean weapons queued to buy, negative values mean weapons queued to sell, and zero is neutral;
  - top-level buy buttons queue pending buys from cheapest eligible current-market seller stock;
  - the same queued trade/review/confirm path is used for weapons and fighter LPCs; cargo mutation branches only at the final `StockPurchaseService` add/remove/report step.
  - visible Seller rows were removed from the Buy and Review GUIs. Generic buy allocation still tracks source stock internally through the quote/purchase services, but the user-facing row model should not reintroduce seller-detail sections unless that feature is explicitly reopened.
  - sell buttons queue pending sells from player cargo only, not broader owned stock that may include market storage;
  - row-level buy/sell adjustment buttons first unwind the opposite queued plan before adding a real buy/sell. Example: if 5 weapons are queued to buy, row `-1` / dynamic `-5` remove those queued buys rather than requiring existing player cargo.
  - queued buys/sells are held in the popup until the user opens `Review Trades`;
  - planned trades are quoted and confirmed in one shared execution order: sells first, explicit seller-specific buys second, generic cheapest buys last. This lets planned sales fund planned buys and prevents generic buy plans from silently consuming stock reserved for explicit seller choices.
  - `Purchase All Until Sufficient` queues cheapest-first current-market buys needed to bring each listed weapon up to desired stock, without buying beyond desired stock, while respecting existing planned trades, current credits, cargo space, and seller stock;
  - `Sell All Until Sufficient` queues inventory sales only where the post-trade stock level remains sufficient;
  - `Reset All Trades` and per-row `Reset` clear planned trades without mutating cargo;
  - pending-trade mutation belongs in `StockReviewPendingTrades`. Keep merge/reset/clear/executed-removal behavior centralized there rather than rebuilding ad hoc list surgery in the panel.
  - the Review GUI groups planned trades under expandable `Buying` and `Selling` table headings, then uses `Confirm Trades` / `Go Back`;
  - expanded review weapon rows show stock cells, the same combined `Buying` / `Selling` plan cell used by the Buy GUI, and weapon data rows;
  - Review GUI opens in a narrower parent dialog than the Buy GUI. Width-sensitive review changes should use `StockReviewStyle.REVIEW_MODAL` / `REVIEW_LIST`, not the full trade modal constants. The review `Storage` cell must stay tied to the trade-screen storage width through `StockReviewStyle.REVIEW_STOCK_CELL_WIDTH = STOCK_CELL_WIDTH`.
  - only `Confirm Trades` mutates cargo, checks player credits/cargo space/sell availability, and rebuilds the popup snapshot afterward;
  - this avoids the awkward immediate recategorization where buying one `No Stock` weapon moves it out of that category before the user finishes shopping;
  - forced vanilla cargo core close/reopen is kept only as a fallback because direct cargo mutation while the trade grid is open can leave stale slot views behind;
  - direct local-market cargo mutations are followed by a best-effort `SubmarketPlugin.reportPlayerMarketTransaction(...)` callback with bought/sold cargo and line-item data, so vanilla/modded submarket listeners and black-market trade-mode side effects have a chance to run. Runtime testing currently suggests expected black-market penalties fire, so do not add extra suspicion/reputation simulation unless testing proves the callback path is insufficient;
  - local sells should deposit into the black market when the Black Market toggle is enabled, otherwise into the best eligible non-black trade submarket. After the transaction callback, WP reconciles the touched submarket cargo to the exact pre-sale count plus sold quantity because vanilla/modded submarket callbacks can normalize stock after WP mutates it.
  - Fixer's Market buys remain virtual WP transactions and intentionally do not report to a real submarket plugin. Sector Market buys remove stock from real remote market cargo and report a best-effort transaction to the touched remote submarket. Remote-mode sells use the current local market buyer with black market disabled.
- Popup category layout:
  - stock categories start collapsed;
  - headings are flat full-width peer rows, not nested checkboxes;
  - weapon rows, nested section rows, and scroll indicators are all explicit row descriptors rather than ad hoc tooltip paragraphs.
- Popup visual rules:
  - WP intentionally mirrors the accepted ACG palette in `StockReviewStyle`: red/cancel for No Stock and sell/decrement controls, yellow/load for Insufficient Stock rows, green/confirm for Sufficient Stock and buy/increment controls, purple for bulk trade controls, dark gray collapsible headings/cells, black neutral action rows, and gray text only for disabled controls.
  - Commit `a02e507` was user-confirmed as the reference point where stock-review indentation and button right-edge sizing finally worked. If future changes break nested row alignment, compare against that commit before trying a new layout theory.
  - Use white/default-font text for ordinary popup text and buttons unless a specific disabled/locked convention applies.
  - The three top stock category headings use their red/yellow/green fills. Nested toggle headings use the ACG dark-gray collapsible heading fill.
  - WP-owned row fills sit behind Starsector buttons while button backgrounds are dimmed, intentionally recreating ACG's inner dimmed rectangle with brighter outer row fill.
  - Weapon rows, review rows, and button hitboxes use white grid borders. Indented spacer regions must not draw borders.
  - Nested stock-review sizing must be treated as a simple indent stack. The Buy GUI hierarchy is `Weapons/Wings -> No/Insufficient/Sufficient Stock -> weapon/wing row -> Basic/Advanced Info -> data row`, with one equal indent step added at each level. If a parent row has visible width `X` and one indent unit is `Y`, a child row starts one indent deeper and has width `X - Y`; a grandchild row starts two indents deeper and has width `X - 2Y`. The right edge must align with the parent row. Do this by adding an invisible borderless indent spacer and reducing the child component width by the same indent, not by shifting a full-width component right.
  - Expanded weapon rows now contain `Basic Info` and `Advanced Info` nested toggle headings. Their data rows are LabelTextComponents one level deeper than the heading and use the shared indent-minus-width layout path.
  - Data LabelTextComponents use a 65% label / 35% value split so long labels such as `EMP/Second` and `Turn Rate/Second` have enough width while compact values stay tight.
  - Weapon entries should keep this order: weapon label, `Storage`, `Price`, `Buying`/`Selling`, dynamic sell step, `-1`, `+1`, dynamic buy step, `Sufficient`, `Reset`.
  - Width increases for right-side row cells such as `Storage`, `Price`, or `Buying`/`Selling` should come out of the weapon toggle-heading width. The weapon label area has slack; do not compensate by widening the popup or squeezing other fixed cells unless explicitly requested.
  - Right-side reserve constants must equal the actual rendered cell block width, including only the gaps between sibling cells that are not already counted inside a grouped control block. If this reserve is too large, nested toggle headings such as `Basic Info` will appear center-aligned or too short instead of sharing the parent weapon row's right edge.
  - `Storage` is the full snapshot owned count under the active owned-source policy, including player inventory. When a plan exists, append the signed pending delta, e.g. `Storage: 6 [-2]` or `Storage: 6 [+2]`.
  - The No Stock dummy row is a deliberate worst-case row-width test. It uses `Suzuki-Clapteryon Thermal Prokector... (+)`, `Storage: 99+`, `Price: 99,999+\u00a2`, and `Selling: 99+ [999,999+\u00a2]`. Real rows should cap displayed storage and plan counts at `99+`, price at `99,999+\u00a2`, and plan totals at `999,999+\u00a2`; fixed cells should be just large enough for those caps, with only the weapon-name cell absorbing spare width.
  - The `Storage` cell is intentionally wider than the other compact stock cells and left-aligned with normal WP internal text padding for readability.
  - Top-level stock headings summarize the visible category as `No Stock [Weapon Types: N][Selling: N][Buying: N]` and equivalent labels for insufficient/sufficient stock. `Weapon Types` is the count of visible weapon rows in that category; `Selling` and `Buying` are queued unit totals and must be counted separately, not netted against each other.
  - `Price` is intentionally wider than the original compact cell; long comma-grouped prices should fit before reclaiming space from other action cells.
  - `Price` in the Buy GUI is the cheapest currently purchasable unit buy price after current queued buys have consumed cheaper stock. If no buy price remains because the row is sell-only or stock has been fully queued, fall back to the best legal player-cargo sell value for the active market/black-market setting. Format prices with comma-grouped credits and cap compact row prices at `99,999+\u00a2`.
  - Keep `Price` as the user-facing and code-facing name for unit weapon price. `StockSortMode.fromConfig(...)` still accepts the old `COST` value as a compatibility alias, but new code/config should use `PRICE`.
  - `Buying` / `Selling` is the signed planned trade for that weapon and includes the full planned trade value in brackets, e.g. `Buying: 5 [50,000\u00a2]`. Positive planned quantities use yellow, negative planned quantities use purple, and zero uses gray.
  - Current trade-side color rule: buy-side cells/actions use yellow, sell-side cells/actions use purple, and general controls such as Sort, Source, Black Market, Filters, Colors, and Reset All Trades use the dark-gray weapon-heading color.
  - The dynamic sell/buy step buttons replace the old fixed `-10` / `+10` when fewer than ten additional weapons can be sold/bought. If one or fewer remain, the dynamic step stays visually as disabled `-10` / `+10` because the separate `-1` / `+1` button handles the one-item case.
  - `Price` and neutral `Buying: 0` cells use the normal gray cell background. Profit cells use green/confirm. Buy/increment buttons are green, sell/decrement buttons are red, bulk trade buttons are purple, and disabled buttons use gray text.
  - `Sufficient` adjusts the weapon to barely sufficient status, buying if there is a deficit and selling if there is an excess.
  - `Sufficient` uses the current queued plan as part of its calculation and uses sell/red styling when the sufficient adjustment would reduce the plan or sell excess.
  - Disabled controls should render as inert WP-owned shells with gray text and disabled fill, not as disabled Starsector buttons. Starsector's disabled-button hover can darken/highlight inconsistently and should not be used for WP action cells.
  - The `Colors` top-row button opens the in-popup Debug Colors screen. Temporary changes mutate the runtime WP palette until restart; Permanent mode also writes the selected RGB values to Starsector common storage as `WP_debugGuiColors.json`. Debug samples, RGB incrementors, Confirm/Apply/Restore/Cancel, and the variable selector must stay on the shared WP row/button path.
  - The old visible `Refresh` and `Mode` buttons were removed. Sort/source changes and trade actions already rebuild the snapshot/content shell through explicit actions.
  - The Make Trades and Review Trades screens intentionally omit the old title/status text box. That vertical space belongs to the main weapon list. Filter and color-debug screens still keep a title/status header because those screens need orientation text.
  - Credit labels should use the Starsector-supported cent-sign glyph (`\u00a2`, font char id 162) instead of the plain `cr` suffix. Longer campaign messages may still say `credits` when that reads better.
  - `Warning`, `Tariffs Paid`, `Credits Available`, and `Cargo Space Available` belong in fixed bottom summary rows above the footer buttons on trade/review screens, not inside the scrollable weapon list. The warning row starts at `None` and otherwise displays the most recently triggered trade warning.
  - `Tariffs Paid` is the markup paid above true base weapon value for planned buys, with an average multiplier label such as `[avg 4.0x]`. It uses the red/cancel fill when the markup is greater than zero and gray when zero.
  - `Credits Available` and `Cargo Space Available` value cells include the current value plus a signed bracketed delta, e.g. `500,000\u00a2 [-100,000\u00a2]` when buying or `500,000\u00a2 [+100,000\u00a2]` when selling. Buying/negative-available deltas use the buy-side yellow fill, selling/positive-available deltas use the sell-side purple fill, and unchanged rows use gray.
  - Trade warnings currently include: `Not enough cargo capacity`, `Not enough credits`, `Credit balance at <5% of initial balance`, and `Cargo capacity at <5% of total capacity`. Plan-changing actions refresh the warning back to `None` when no warning condition remains.
  - Performance-sensitive trade math should go through `StockReviewTradeContext`, which caches pending buy/sell quantities, per-weapon costs, total cost, cargo-space delta, current credits, cargo space, and affordability probes for the current render/controller action.
  - Market quote/pricing work should go through `StockReviewQuoteBook`. It caches sorted buyable seller lists, line quotes, seller allocations, sell prices, and fallback cargo-space values so render/controller paths do not repeatedly copy/sort submarkets or scan player cargo.
  - Whole-plan pricing should use `StockReviewPortfolioQuote`, not independent line quotes. Generic and seller-specific buys can overlap the same market stock, so quote the full planned portfolio with per-seller remaining stock consumed once.
  - Source-specific and generic buy enabled states should go through `StockReviewTradeContext`; they must account for existing planned trades, seller stock, credits, and cargo space.
  - Quote result data uses explicit top-level GUI classes (`StockReviewQuote`, `StockReviewSellerAllocation`) rather than anonymous/local/lambda helpers or stale purchase-preview naming.
  - `WeaponStockSnapshot` keeps cached all-record and weapon-id maps. Do not reintroduce repeated `getAllRecords()` reconstruction or linear id scans in render loops.
- Popup list filtering:
  - Category counts and weapon rows are filtered to records with at least one currently buyable unit from the active stock source or at least one player-inventory unit that can be sold. The popup is for shopping/selling, not for showing unavailable desired weapons or storage-only weapons.
  - Weapon stock summary text uses `Storage` for total owned stock under the active owned-source policy, including player inventory. Starsector APIs often expose combined cargo/storage counts, so keep row inclusion tied to player inventory plus buyable source stock rather than broad storage ownership.
- Popup availability:
  - `F8` is now gated to `SectorAPI.getCurrentlyOpenMarket()` plus at least one weapon currently buyable under the current black-market setting. It should not open from looting, non-trade planet contexts, or markets with only locked/unbuyable weapon stock.
- Purchase refresh:
  - The current preferred buy path does not force-close/reopen the vanilla cargo core after purchase; it mutates cargo, then rebuilds the popup snapshot in place. The old forced core refresh remains behind `StockReviewStyle.REFRESH_VANILLA_CORE_AFTER_PURCHASE` in case vanilla trade-grid stale-slot corruption still reproduces.
  - Local-market transactions are still not guaranteed to be perfectly vanilla-identical. WP now reports `PlayerMarketTransaction` to the touched submarket plugin, but runtime testing should still confirm tariff, suspicion, reputation, economy-impact, and modded-listener behavior.
- Normal mod-side code owns all campaign state:
  - `WeaponsProcurementModPlugin` registers `WeaponsProcurementCountUpdater` as a transient script on game load.
  - `WeaponsProcurementCountUpdater` runs while paused, computes player-cargo plus accessible-storage totals, and publishes JVM `System` properties.
  - `WeaponsProcurementFixerCatalogUpdater` runs while paused, but uses campaign-clock elapsed days for its scan interval. It scans immediately after load, then roughly once per in-game day, and stores only simple Java collections in sector persistent data to avoid custom save-object compatibility problems.
  - `WeaponsProcurementBadgeHelper` is embedded in patched core and only reads `System` properties to select a precomposed badge sprite path.
- Active visual path:
  - one precomposed `graphics/ui/wp_total_*.png` badge sprite;
  - bottom-right cell placement from the stable pre-scale coordinate frame;
  - no layered background/text sprites;
  - no runtime badge scaling;
  - no late over-icon render anchor.
- LunaLib is used only by normal mod-side config code, never by the embedded patched-core badge helper:
  - setting id: `wp_update_interval_seconds`;
  - setting id: `wp_enable_patched_badges`;
  - setting id: `wp_enable_dialog_option`;
  - setting id: `wp_enable_fixers_market_tag_inference`, default off;
  - published property: `wp.config.updateIntervalSeconds`;
  - published property: `wp.config.patchedBadgesEnabled`;
  - published property: `wp.config.dialogOptionEnabled`;
  - published property: `wp.config.fixersMarketTagInferenceEnabled`;
  - update interval default: `0.20`, clamped to `0.05..2.00`.
- `wp_enable_patched_badges=false` makes the embedded helper return `null`, so a patched core jar will skip badge rendering while the normal popup continues to work.

## ACG-Derived GUI Rules For WP

These are the ACG Starsector UI lessons that matter for the Weapon Stock Review popup. Keep them in mind when extending WP; they are runtime-tested Starsector GUI behavior, not just style preference.

- Keep the clean popup architecture explicit:
  - services build immutable-ish stock snapshots;
  - state stores expansion, scroll, filters, sort, and source toggles;
  - renderers draw from state plus snapshot only;
  - controllers mutate cargo or state and request targeted refreshes.
- Do not return to long, nested `TooltipMakerAPI` row piles for the main popup. The ACG lesson is that one explicit custom-panel/list model is easier to refresh, scroll, and reason about than removing and recreating tooltip content inside the same visual stack.
- Share the same popup/list/control foundation across trade, storage, and any future market host. Host differences should be thin adapters for opening, closing, market discovery, vanilla UI refresh, and input forwarding. Duplicating the GUI per host is expected to drift.
- Buttons and toggle rows should be centralized through shared helpers/templates. A row caller should choose a semantic action/color/enabled state; the helper should own label color, disabled behavior, guarded callbacks, hover behavior, sounds, tooltip suppression, and action registration.
- Current shared GUI-helper ownership:
  - `WimGuiStyle` owns ACG-derived button dimming, shared text metrics, disabled colors, uncoloured-button defaults, semantic confirm/cancel/load/save colors, modal panel colors, heading colors, and generic row border defaults.
  - `WimGuiDialogPanel` / `WimGuiDialogDelegate` own reusable `CustomVisualDialogDelegate` wiring for WP modal panels.
  - `WimGuiDialogOpener` owns reusable `showCustomVisualDialog(...)` calls.
  - `WimGuiCampaignDialogHost` owns reusable current campaign sector/UI/dialog/market/player-cargo lookup, safe campaign message reporting, and guarded vanilla cargo-core refresh for modal launchers.
  - `WimGuiDialogTracker` / `WimGuiPendingDialog` own reusable modal open-state and pending-reopen state.
  - `WimGuiHotkeyLatch` owns edge-triggered keyboard hotkey handling.
  - `WimGuiNoopCoreInteractionListener` owns reusable no-op core-UI dismissal callbacks, avoiding anonymous listener classes in runtime-sensitive GUI code.
  - `WimGuiModalInput` / `WimGuiInputResult` own reusable modal input handling for Escape-to-close, list wheel scrolling, and event-gated button polling.
  - `WimGuiModalPanelPlugin` owns reusable modal panel lifecycle: dialog init, content replacement, input dispatch, button dispatch, close callback handling, render failure handling, and current scroll-bound state.
  - `WimGuiPanelPlugin` owns reusable row/container fill and border drawing.
  - `WimGuiContentPanel` owns reusable in-place custom-panel replacement: remove old content, create fresh content, then attach after rendering.
  - `WimGuiControls` owns reusable button shells, disabled button behavior, overlaid labels, info-cell rendering, shared header/body text rendering, and generic left-to-right button-row placement.
  - `WimGuiButtonSpec` owns semantic button-row descriptors so callers supply width/label/action/enabled/color rather than hand-written x-position chains.
  - `WimGuiButtonSpecs` owns reusable button-spec list construction so renderers do not hand-roll mutable button-list setup.
  - `WimGuiSemanticButtonFactory` owns reusable semantic button-spec creation when a popup wants one shared border/default disabled behavior across action, footer, and bulk buttons.
  - `WimGuiTooltip`, `StockReviewTooltips`, and the tooltip fields on shared button/list-row/row-cell specs own popup tooltips. Add tooltips through the shared spec path so toggle headings, disabled buttons, info cells, row buttons, footer buttons, and future screens get consistent behavior.
  - `WimGuiButtonBinding` / `WimGuiButtonPoller` own event-gated Starsector button fallback polling.
  - `WimGuiModalActionRow` owns reusable top action-row placement below a modal header.
  - `WimGuiModalHeader` owns reusable title/status header panel rendering.
  - `WimGuiModalFooter` owns reusable bottom-button placement for left button rows, right edge buttons, and edge-button pairs.
  - `WimGuiToggleHeading` owns standard `(+)/(-)` and counted heading label formatting.
  - `WimGuiScrollIndicator` owns shared ASCII scroll indicator labels.
  - `WimGuiListBounds` owns reusable scrollable-list hitbox data and Starsector root-coordinate containment checks.
  - `WimGuiRowCell` owns reusable fixed-width info/action row-cell data, standard action-cell disabled text state, row-cell list construction, and total cell-block width calculation; `WimGuiControls.addRowCell(...)` owns rendering those cells as either info panels or bound buttons.
  - `WimGuiListRow` owns reusable list-row descriptors: label, colors, border, indent, main action, alignment, right-side cells, and top-gap metadata.
  - `WimGuiListRowRenderer` owns reusable fixed-height list-row painting; screen-specific renderers should pass dimensions/style constants rather than recreating row panels, button hitboxes, and cell placement.
  - `WimGuiText` / `WimGuiTextLayout` own word-aware fitting/wrapping, long-token handling, weak-line-ending rebalancing, line counts, and growable row-height calculation.
  - `WimGuiScroll` / `WimGuiScrollSlice` own useful vertical slicing, redundant one-row scroll offset avoidance, and default wheel-scroll step.
  - `WimGuiScrollableListState` owns the shared scroll-offset state contract for modal lists.
  - `WimGuiModalLayout` owns reusable modal spacing for heading/body/footer structure, action-row placement, and list-panel height math.
  - `WimGuiModalListLayout` owns reusable modal-list positioning, tight black-panel height calculation, visible-row slicing, extra row-gap accounting, and page-sized scroll deltas.
  - `WimGuiModalListSpec` / `WimGuiModalListRenderer` / `WimGuiModalListRenderResult` own reusable central-list rendering: black bordered list panel creation, scroll indicator rows, row rendering, useful offset preservation, and list bounds reporting.
  - `StockReviewTradeWarnings` owns stock-review warning text, initial credit/cargo baselines, low-balance/low-capacity thresholds, and purchase-all partial-fill warning detection. Keep warning-policy changes there rather than growing `StockReviewPanelPlugin`.
  - Screen-specific renderers should compose these helpers rather than recreating button, text, or scroll math locally.
- The Weapon Stock Review popup should keep moving toward the ACG custom-list warning/manage-list shape where practical: heading/status at top, central black list body with white border, scroll indicators as row controls, tight body height when few rows are visible, spare body space allocated above the list rather than below it, and bottom action buttons with shared semantic colors.
- Starsector area-checkbox colors are counterintuitive:
  - `base` behaves like hover/glow;
  - `bg` behaves like checked fill/border;
  - built-in label coloring is limited, so WP should keep using controlled row text instead of relying on checkbox labels for complex rows.
- Starsector dims idle button interiors heavily. Raw RGB values can look much darker in game, while hover/glow is closer to raw RGB. For WP, keep hover/glow equal to the idle/base color unless a runtime-tested exception is deliberately accepted.
- Avoid bare `addAreaCheckbox(...)` visuals for action rows. If WP uses area checkboxes for a future row type, give the row an owned background/fill so idle colors do not degrade into only a border or ring.
- Use the imported ACG palette consistently:
  - No Stock category rows: cancel red;
  - Insufficient rows: load yellow;
  - Buy/increment buttons: confirm green;
  - Sell/decrement buttons: cancel red;
  - Bulk trade buttons: purple;
  - Sufficient category rows: confirm green;
  - nested toggle headings: dark gray;
  - neutral available rows: black/dark action background;
  - disabled/locked rows: gray text with disabled/dark shell and no meaningful hover.
- Ordinary WP popup text should stay white/default-font. Use gray text only for disabled, locked, or unavailable states unless the user explicitly asks for another convention.
- Scrollable lists need one shared math path:
  - preserve scroll offsets and expanded headings across rebuilds;
  - consume wheel/input events when the custom list handles them;
  - never reserve scroll indicators when all rows fit;
  - page/indicator movement should use actual visible row capacity;
  - indicators should share the same row width, padding, and vertical rhythm as the list they scroll;
  - ASCII indicators are safer than arrow glyphs in this UI.
  - keep this behavior in `WimGuiScroll` / `WimGuiModalListLayout`, not local per-screen copies.
- State-changing filters, source toggles, and sort changes should refresh the existing content shell and preserve expansion state. Replacing the full root popup should be reserved for real host/lifecycle changes.
- If WP adds modal popups, use the ACG three-section template: heading, body, bottom buttons. Width, padding, heading height, button height, section gaps, and the 80%-of-screen max-height cap should be shared constants; only body content and computed body height should vary.
- Do not rely on click-out-to-close for modals. Escape and explicit Close/Cancel are sufficient. If outside-click behavior is added later, use raw-coordinate inside/outside checks rather than binding a full-screen backdrop directly to Cancel.
- Modal/background input shielding matters in Starsector. Input consumption alone can be too late to stop hover sounds/tooltips behind a modal, so future WP modals should disable or mute non-modal controls while open and restore their previous enabled state afterward.
- Avoid `addParaWithMarkup()` and highlighted `addPara(...)` overloads for row labels and weapon text. `%` can be treated as formatter syntax, and markup paths caused literal markup/clipping problems in ACG. Prefer shared plain-text fitting/wrapping helpers.
- Text wrapping should avoid weak line endings such as `as`, `as the`, `and`, `of`, and `to`. Long body copy should use one shared wrapping helper rather than local fixes in each popup.
- Prefer Starsector-owned/default font selectors over raw font asset paths. A raw font path can compile and still crash in a specific custom UI entry point.
- Keep button order consistent if WP adds confirmation modals: `Confirm` green, `Apply`/secondary purple where applicable, `Delete`/destructive-yellow where applicable, `Cancel` red, left to right.
- Left click should cycle forward and right click should cycle backward for future cycling option buttons, with both directions routed through the same button/action abstraction and sound handling.
- For performance, build render-ready data during snapshot creation. Do not repeatedly call settings/spec lookups, classify stock, or scan cargo inside row rendering loops.
- Clean code should still respect Starsector classloading reality. Compile/jar success is not enough after GUI helper extraction. Prefer stable explicit classes over anonymous/local/lambda-generated classes in runtime-sensitive UI or patched-helper paths, and inspect/test affected entry points after helper placement changes.
- Runtime trade helpers follow the same rule. For example, `StockPurchaseService` uses an explicit purchase-source comparator rather than a generated `$1` anonymous comparator class, and the live-jar validator rejects the stale generated class.
- After adding new GUI helper classes, verify the live jar contains those classes and restart Starsector before testing. Starsector's script classloader can keep an older jar index for a running process; hot-copying a jar while the game is already open can still produce `NoClassDefFoundError` for new classes.
- After renaming or removing nested classes, keep a short-lived compatibility shim when the old name may already be referenced by a loaded outer class. A running Starsector process can hold stale bytecode such as `WeaponStockSnapshotBuilder$CostComparator` even after the jar has been rebuilt with a renamed helper.
- Keep `tools\validate-live-gui-classes.ps1` current when helper ownership changes. It should check required current helpers, intentional nested helper interfaces, and forbidden stale helper classes, because stale or missing jar entries can hide migration regressions until runtime.
- The patched badge path must remain isolated from the popup. The badge helper should keep asking only for precomputed badge sprite state; it must not own stock logic, desired-stock logic, market scanning, buying, or GUI behavior.

## Count Bridge

Published properties:

- `wp.weapon.<weaponId>.player`
- `wp.weapon.<weaponId>.storage`
- `wp.fighter.<wingId>.player`
- `wp.fighter.<wingId>.storage`
- `wp.counts.ready`
- `wp.counts.updatedAt`

Counting rules:

- Weapon ids come from `SectorAPI.getAllWeaponIds()`.
- Fighter LPC ids come from `SectorAPI.getAllFighterWingIds()`.
- Player weapon counts use `CargoAPI.getWeapons()` data.
- Player fighter counts use `CargoAPI.getFighters()` data.
- Storage counts include only markets where `Misc.playerHasStorageAccess(market)` is true.
- Storage cargo comes from `Misc.getStorageCargo(market)`.
- Market stock for sale is intentionally not counted.
- Fighter inventory counts must use cargo fighter counts, not `FighterWingSpecAPI.getNumFighters()`, which is craft-per-wing metadata.

## Badge Mapping

- Total = player count + accessible storage count.
- Display:
  - `0`: red `0`;
  - `1..9`: yellow exact number;
  - `10..98`: green exact number;
  - `>=99`: green `99+`;
  - missing/invalid bridge state: `wp_total_err.png`.
- Badge assets are generated by `tools/generate-total-badges.ps1`.
- Asset and JSON validation is handled by `tools/validate-total-badges.ps1`.
- Patched `CargoStackView` validation is handled by `tools/validate-cargo-stack-view-patch.ps1`.
  - It compiles the ASM patcher and runs `Verify` mode against the active `starfarer_obf.jar`.
  - The report checks the target class/method, WEAPONS branch guard, embedded helper class, one WEAPONS total-helper call, one non-WEAPONS total-helper call, one WEAPONS badge sprite render call, and absence of stale diagnostic/marker/count-path patches.

## Patcher Rules

- Patch exactly `CargoStackView.renderAtCenter(FFF)V`.
- Preserve the stable pre-scale one-sprite render path.
- Do not reintroduce:
  - `CargoWeaponMarkerHook`;
  - raw GL rendering from helper classes;
  - duplicate `weaponIcon` draws;
  - three-square diagnostic badge calls;
  - late over-icon injection;
  - layered background/text badge rendering;
  - runtime badge scaling;
  - direct `Global`, sector, fleet, cargo, or storage calls in patched bytecode.
- `CargoStackViewPatcher` still keeps legacy signatures for refusal checks even though the old source/assets are removed.
- Restore before patching when the helper call is already present.
- The patcher must also reject pre-rebrand `weaponinventorymod/internal/WeaponInventoryBadgeHelper` calls. During the Weapons Procurement rebrand, a backup was briefly created from an already-patched core jar because the old package helper was not recognized as stale; keep that guard in place.
- If restore/patch fails with a file-lock error, check for a lingering Starsector JVM using `C:\Games\Starsector\jre\bin\java.exe`.

## Durable Lessons

- `javap`-verify runtime JVM descriptors before injecting method calls. Earlier direct count injection crashed because `SectorAPI.getPlayerFleet()` was injected with the wrong return package.
- Embedded helper classes called by patched core must be visible to the core classloader. If helper code ever uses nested/anonymous/lambda classes, the patcher must embed all generated companion classes; prefer single-class helpers for this path.
- `Global.getSector()` returned null from the embedded patched-core helper context, even while campaign UI was rendering. Keep campaign-state computation in normal mod code.
- Bytecode presence is not proof of visible UI. In-game visibility decides whether the injection point and coordinate frame are usable.
- The commodity/resource rank-marker path and Demand Indicators behavior are not a direct weapon/fighter solution; weapon and fighter stacks use different branch flow.
- The late shared/over-icon anchor caused visual regressions. The validated baseline is the pre-scale one-sprite path, even if badges can render behind parts of the vanilla icon.

## Build And Deploy

Typical test-ready sequence:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\generate-total-badges.ps1
powershell -ExecutionPolicy Bypass -File .\build.ps1
powershell -ExecutionPolicy Bypass -File .\tools\cargo-stack-view-patcher.ps1 -Mode Restore
powershell -ExecutionPolicy Bypass -File .\tools\cargo-stack-view-patcher.ps1 -Mode Patch
powershell -ExecutionPolicy Bypass -File .\tools\validate-cargo-stack-view-patch.ps1
robocopy data "C:\Games\Starsector\mods\Weapons Procurement\data" /MIR
robocopy graphics "C:\Games\Starsector\mods\Weapons Procurement\graphics" /MIR
robocopy jars "C:\Games\Starsector\mods\Weapons Procurement\jars" /MIR
Copy-Item mod_info.json "C:\Games\Starsector\mods\Weapons Procurement\mod_info.json" -Force
powershell -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -ExecutionPolicy Bypass -File .\tools\validate-total-badges.ps1
```

Manual validation:

- Launch/load save.
- Open a market trade screen.
- Press `F8` to open Weapon Stock Review.
- Confirm the popup groups weapons under No Stock, Insufficient Stock, and Sufficient Stock.
- Confirm weapon entries show stable `Storage`, unit `Price`, and planned `Buying`/`Selling` cells while queued changes are adjusted.
- Confirm `Source` and `Black Market` buttons rebuild the snapshot without layered stale text and without closing/reopening the popup.
- Confirm `Sort` cycles through `Stock`, `Name`, and `Price` without collapsing headings.
- Confirm weapon rows expand directly into weapon data rows.
- Confirm mouse-wheel scrolling and clickable `^     ^     ^     ^     ^` / `v     v     v     v     v` indicators preserve state and do not appear when all rows fit.
- Confirm `+1`/dynamic buy-step works from top-level rows, with credits/space failures blocked.
- Confirm no crash.
- Confirm commodities remain vanilla.
- Confirm weapon and fighter LPC stacks show one bottom-right badge.
- Confirm totals include player cargo plus all accessible storage.
- Confirm buy/sell/storage moves update while paused.
