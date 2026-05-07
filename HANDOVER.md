# Weapon Inventory Mod Handover

## Current State

- The mod renders ownership-count badges directly inside vanilla market/trade cargo cells for weapon stacks and fighter LPC stacks.
- Exact in-cell rendering is implemented by deterministically patching `com/fs/starfarer/campaign/ui/trade/CargoStackView.renderAtCenter(FFF)V` in `starfarer_obf.jar`.
- A clean normal-mod frontend is now being added as the primary product: `F8` opens a Weapon Stock Review popup from an active market/storage interaction dialog.
- The popup is independent of the `CargoStackView` patcher. The clean UI should keep working if the patcher is removed.
- Terminology:
  - `Buy GUI` means the main `F8` stock-review screen.
  - `Review GUI` means the planned-trade review screen opened from the Buy GUI.
  - `main headings` / `top level headings` means `No Stock`, `Insufficient Stock`, and `Sufficient Stock`.
  - `weapon entries` means rows under those main headings, such as `Light Needler (-)` plus stock, plan, cost/profit, and buy/sell controls.
- Popup configuration lives in `data/config/weapon_inventory_stock.json`:
  - include/exclude accessible storage in current stock calculations;
  - include/exclude black market stock;
  - desired stock defaults by weapon size;
  - per-weapon desired/ignored overrides.
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
  - Clickable rows/buttons now use blank Starsector button text with WIM-rendered labels layered separately, so hover/base colors stay ACG-like while visible text remains white/gray under WIM control.
- Popup scope:
  - The old display-mode button and config mode were removed. The Buy GUI should show only weapons that are buyable from the active stock source or present in player inventory and therefore sellable through the GUI.
  - Stored-only weapons should not create rows. The `Storage` cell may still show full owned stock, including accessible storage, for a weapon that appears because it is buyable or in player inventory.
- Global Weapon Market:
  - The Buy GUI now has a `Source: Local/Global` toggle. Local source behaves like the normal current-market review. Global source is a virtual seller, not a real submarket inserted into vanilla cargo.
  - Initial global eligibility is intentionally conservative and live-scan based: `GlobalWeaponMarketService` scans all current economy markets and includes a weapon if it appears in any non-storage/non-local-resources market cargo that WIM can see. Eligible weapons appear with 999 virtual stock.
  - Global buy prices are multiplied by the Luna setting `wim_global_market_price_multiplier`, default `4.0`. This markup applies only to the virtual global buy source.
  - Global purchases use `StockPurchaseService.buyVirtualGlobal(...)`, which charges credits and adds weapons directly to player cargo without removing anything from real markets.
  - Global sales use `StockPurchaseService.sellVirtualGlobal(...)`, which removes weapons from player cargo and pays normal base value without adding stock to a real market. Do not apply the global buy markup to sells.
  - Optional tag/faction inference is Luna-gated by `wim_enable_global_market_tag_inference`. Keep it separate from the live-scan path so it can be disabled if it admits secret/restricted weapons. The inference path uses active market factions' explicit `FactionAPI.getWeaponSellFrequency()` entries first, falls back to `getKnownWeapons()` only when a faction has no sell-frequency data, and excludes obvious special tags such as `restricted`, `no_dealer`, `omega`, `dweller`, `threat`, and codex-hidden/unlockable markers.
- Popup sorting:
  - `Need`: lowest stored-outside-inventory count first, then cheapest current buy price, then weapon name;
  - `Name`: weapon name first, then need, then price;
  - `Price`: cheapest current buy price first, then most needed, then weapon name.
- Popup purchase flow:
  - weapon entries in the Buy GUI use a signed `Plan`: positive values mean weapons queued to buy, negative values mean weapons queued to sell, and zero is neutral;
  - top-level buy buttons queue pending buys from cheapest eligible current-market seller stock;
  - expanded Seller rows may still queue a pending buy from that specific submarket;
  - sell buttons queue pending sells from player cargo only, not broader owned stock that may include market storage;
  - queued buys/sells are held in the popup until the user opens `Review Trades`;
  - planned trades are quoted and confirmed in one shared execution order: sells first, explicit seller-specific buys second, generic cheapest buys last. This lets planned sales fund planned buys and prevents generic buy plans from silently consuming stock reserved for explicit seller choices.
  - `Purchase All Until Sufficient` queues cheapest-first current-market buys needed to bring each listed weapon up to desired stock, without buying beyond desired stock, while respecting existing planned trades, current credits, cargo space, and seller stock;
  - `Sell All Until Sufficient` queues inventory sales only where the post-trade stock level remains sufficient;
  - `Reset All Trades` and per-row `Reset` clear planned trades without mutating cargo;
  - pending-trade mutation belongs in `StockReviewPendingTrades`. Keep merge/reset/clear/executed-removal behavior centralized there rather than rebuilding ad hoc list surgery in the panel.
  - the Review GUI groups planned trades under expandable `Buying` and `Selling` table headings, then uses `Confirm Trades` / `Go Back`;
  - expanded review weapon rows show stock cells, trade quantity, cost/profit, `Weapon Data`, and seller allocations for buys;
  - only `Confirm Trades` mutates cargo, checks player credits/cargo space/sell availability, and rebuilds the popup snapshot afterward;
  - this avoids the awkward immediate recategorization where buying one `No Stock` weapon moves it out of that category before the user finishes shopping;
  - forced vanilla cargo core close/reopen is kept only as a fallback because direct cargo mutation while the trade grid is open can leave stale slot views behind;
  - direct local-market cargo mutations are followed by a best-effort `SubmarketPlugin.reportPlayerMarketTransaction(...)` callback with bought/sold cargo and line-item data, so vanilla/modded submarket listeners and black-market trade-mode side effects have a chance to run;
  - global-market buys/sells remain virtual WIM transactions and intentionally do not report to a real submarket plugin.
- Popup category layout:
  - stock categories start collapsed;
  - headings are flat full-width peer rows, not nested checkboxes;
  - weapon rows, nested section rows, seller rows, and scroll indicators are all explicit row descriptors rather than ad hoc tooltip paragraphs.
- Popup visual rules:
  - WIM intentionally mirrors the accepted ACG palette in `StockReviewStyle`: red/cancel for No Stock and sell/decrement controls, yellow/load for Insufficient Stock rows, green/confirm for Sufficient Stock and buy/increment controls, purple for bulk trade controls, dark gray collapsible headings/cells, black neutral action rows, and gray text only for disabled controls.
  - Use white/default-font text for ordinary popup text and buttons unless a specific disabled/locked convention applies.
  - The three top stock category headings use their red/yellow/green fills. Nested toggle headings such as `Weapon Data` and `Sellers` use the ACG dark-gray collapsible heading fill.
  - WIM-owned row fills sit behind Starsector buttons while button backgrounds are dimmed, intentionally recreating ACG's inner dimmed rectangle with brighter outer row fill.
  - Weapon rows, seller rows, review rows, and button hitboxes use white grid borders. Indented spacer regions must not draw borders; nested `Weapon Data` / `Sellers` heading buttons start after the indent so the left indent remains black.
  - Weapon entries should keep this order: weapon label, `Storage`, `Price`, `Buying`/`Selling`, dynamic sell step, `-1`, `+1`, dynamic buy step, `Sufficient`, `Reset`.
  - `Storage` is the full snapshot owned count under the active owned-source policy, including player inventory. When a plan exists, append the signed pending delta, e.g. `Storage: 6 [-2]` or `Storage: 6 [+2]`.
  - `Price` in the Buy GUI is the cheapest currently purchasable unit buy price; if no buy price exists because the row is sell-only, fall back to the best legal player-cargo sell value for the active market/black-market setting. Format prices with comma-grouped credits.
  - `Buying` / `Selling` is the signed planned trade for that weapon and includes the full planned trade value in brackets, e.g. `Buying: 5 [50,000cr]`. Positive planned quantities use green, negative planned quantities use red, and zero uses gray.
  - The dynamic sell/buy step buttons replace the old fixed `-10` / `+10` when fewer than ten additional weapons can be sold/bought. If one or fewer remain, the dynamic step stays visually as disabled `-10` / `+10` because the separate `-1` / `+1` button handles the one-item case.
  - `Price` and neutral `Buying: 0` cells use the normal gray cell background. Profit cells use green/confirm. Buy/increment buttons are green, sell/decrement buttons are red, bulk trade buttons are purple, and disabled buttons use gray text.
  - `Sufficient` adjusts the weapon to barely sufficient status, buying if there is a deficit and selling if there is an excess.
  - Disabled controls should render as inert WIM-owned shells with gray text and disabled fill, not as disabled Starsector buttons. Starsector's disabled-button hover can darken/highlight inconsistently and should not be used for WIM action cells.
  - The `Colors` top-row button opens the in-popup Debug Colors screen. Temporary changes mutate the runtime WIM palette until restart; Permanent mode also writes the selected RGB values to Starsector common storage as `WIM_debugGuiColors.json`. Debug samples, RGB incrementors, Confirm/Apply/Restore/Cancel, and the variable selector must stay on the shared WIM row/button path.
  - The old visible `Refresh` and `Mode` buttons were removed. Sort/source changes and trade actions already rebuild the snapshot/content shell through explicit actions.
  - Performance-sensitive trade math should go through `StockReviewTradeContext`, which caches pending buy/sell quantities, per-weapon costs, total cost, cargo-space delta, current credits, cargo space, and affordability probes for the current render/controller action.
  - Market quote/pricing work should go through `StockReviewQuoteBook`. It caches sorted buyable seller lists, line quotes, seller allocations, sell prices, and fallback cargo-space values so render/controller paths do not repeatedly copy/sort submarkets or scan player cargo.
  - Whole-plan pricing should use `StockReviewPortfolioQuote`, not independent line quotes. Generic and seller-specific buys can overlap the same market stock, so quote the full planned portfolio with per-seller remaining stock consumed once.
  - Seller row `+1` / dynamic buy-step enabled states should also go through `StockReviewTradeContext`; they must account for existing planned trades, seller stock, credits, and cargo space just like the main weapon row buttons.
  - Quote result data uses explicit top-level GUI classes (`StockReviewQuote`, `StockReviewSellerAllocation`) rather than anonymous/local/lambda helpers or stale purchase-preview naming.
  - `WeaponStockSnapshot` keeps cached all-record and weapon-id maps. Do not reintroduce repeated `getAllRecords()` reconstruction or linear id scans in render loops.
- Popup list filtering:
  - Category counts and weapon rows are filtered to records with at least one currently buyable unit from the active stock source or at least one player-inventory unit that can be sold. The popup is for shopping/selling, not for showing unavailable desired weapons or storage-only weapons.
  - Weapon stock summary text uses `Storage` for total owned stock under the active owned-source policy, including player inventory. Starsector APIs often expose combined cargo/storage counts, so keep row inclusion tied to player inventory plus buyable source stock rather than broad storage ownership.
- Popup availability:
  - `F8` is now gated to `SectorAPI.getCurrentlyOpenMarket()` plus at least one weapon currently buyable under the current black-market setting. It should not open from looting, non-trade planet contexts, or markets with only locked/unbuyable weapon stock.
- Purchase refresh:
  - The current preferred buy path does not force-close/reopen the vanilla cargo core after purchase; it mutates cargo, then rebuilds the popup snapshot in place. The old forced core refresh remains behind `StockReviewStyle.REFRESH_VANILLA_CORE_AFTER_PURCHASE` in case vanilla trade-grid stale-slot corruption still reproduces.
  - Local-market transactions are still not guaranteed to be perfectly vanilla-identical. WIM now reports `PlayerMarketTransaction` to the touched submarket plugin, but runtime testing should still confirm tariff, suspicion, reputation, economy-impact, and modded-listener behavior.
- Normal mod-side code owns all campaign state:
  - `WeaponInventoryModPlugin` registers `WeaponInventoryCountUpdater` as a transient script on game load.
  - `WeaponInventoryCountUpdater` runs while paused, computes player-cargo plus accessible-storage totals, and publishes JVM `System` properties.
  - `WeaponInventoryBadgeHelper` is embedded in patched core and only reads `System` properties to select a precomposed badge sprite path.
- Active visual path:
  - one precomposed `graphics/ui/wim_total_*.png` badge sprite;
  - bottom-right cell placement from the stable pre-scale coordinate frame;
  - no layered background/text sprites;
  - no runtime badge scaling;
  - no late over-icon render anchor.
- LunaLib is used only by normal mod-side config code for update interval:
  - setting id: `wim_update_interval_seconds`;
  - setting id: `wim_enable_patched_badges`;
  - published property: `wim.config.updateIntervalSeconds`;
  - published property: `wim.config.patchedBadgesEnabled`;
  - default: `0.20`, clamped to `0.05..2.00`.
- `wim_enable_patched_badges=false` makes the embedded helper return `null`, so a patched core jar will skip badge rendering while the normal popup continues to work.

## ACG-Derived GUI Rules For WIM

These are the ACG Starsector UI lessons that matter for the Weapon Stock Review popup. Keep them in mind when extending WIM; they are runtime-tested Starsector GUI behavior, not just style preference.

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
  - `WimGuiDialogPanel` / `WimGuiDialogDelegate` own reusable `CustomVisualDialogDelegate` wiring for WIM modal panels.
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
  - Screen-specific renderers should compose these helpers rather than recreating button, text, or scroll math locally.
- The Weapon Stock Review popup should keep moving toward the ACG custom-list warning/manage-list shape where practical: heading/status at top, central black list body with white border, scroll indicators as row controls, tight body height when few rows are visible, spare body space allocated above the list rather than below it, and bottom action buttons with shared semantic colors.
- Starsector area-checkbox colors are counterintuitive:
  - `base` behaves like hover/glow;
  - `bg` behaves like checked fill/border;
  - built-in label coloring is limited, so WIM should keep using controlled row text instead of relying on checkbox labels for complex rows.
- Starsector dims idle button interiors heavily. Raw RGB values can look much darker in game, while hover/glow is closer to raw RGB. For WIM, keep hover/glow equal to the idle/base color unless a runtime-tested exception is deliberately accepted.
- Avoid bare `addAreaCheckbox(...)` visuals for action rows. If WIM uses area checkboxes for a future row type, give the row an owned background/fill so idle colors do not degrade into only a border or ring.
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
- Ordinary WIM popup text should stay white/default-font. Use gray text only for disabled, locked, or unavailable states unless the user explicitly asks for another convention.
- Scrollable lists need one shared math path:
  - preserve scroll offsets and expanded headings across rebuilds;
  - consume wheel/input events when the custom list handles them;
  - never reserve scroll indicators when all rows fit;
  - page/indicator movement should use actual visible row capacity;
  - indicators should share the same row width, padding, and vertical rhythm as the list they scroll;
  - ASCII indicators are safer than arrow glyphs in this UI.
  - keep this behavior in `WimGuiScroll` / `WimGuiModalListLayout`, not local per-screen copies.
- State-changing filters, source toggles, and sort changes should refresh the existing content shell and preserve expansion state. Replacing the full root popup should be reserved for real host/lifecycle changes.
- If WIM adds modal popups, use the ACG three-section template: heading, body, bottom buttons. Width, padding, heading height, button height, section gaps, and the 80%-of-screen max-height cap should be shared constants; only body content and computed body height should vary.
- Do not rely on click-out-to-close for modals. Escape and explicit Close/Cancel are sufficient. If outside-click behavior is added later, use raw-coordinate inside/outside checks rather than binding a full-screen backdrop directly to Cancel.
- Modal/background input shielding matters in Starsector. Input consumption alone can be too late to stop hover sounds/tooltips behind a modal, so future WIM modals should disable or mute non-modal controls while open and restore their previous enabled state afterward.
- Avoid `addParaWithMarkup()` and highlighted `addPara(...)` overloads for row labels and weapon text. `%` can be treated as formatter syntax, and markup paths caused literal markup/clipping problems in ACG. Prefer shared plain-text fitting/wrapping helpers.
- Text wrapping should avoid weak line endings such as `as`, `as the`, `and`, `of`, and `to`. Long body copy should use one shared wrapping helper rather than local fixes in each popup.
- Prefer Starsector-owned/default font selectors over raw font asset paths. A raw font path can compile and still crash in a specific custom UI entry point.
- Keep button order consistent if WIM adds confirmation modals: `Confirm` green, `Apply`/secondary purple where applicable, `Delete`/destructive-yellow where applicable, `Cancel` red, left to right.
- Left click should cycle forward and right click should cycle backward for future cycling option buttons, with both directions routed through the same button/action abstraction and sound handling.
- For performance, build render-ready data during snapshot creation. Do not repeatedly call settings/spec lookups, classify stock, or scan cargo inside row rendering loops.
- Clean code should still respect Starsector classloading reality. Compile/jar success is not enough after GUI helper extraction. Prefer stable explicit classes over anonymous/local/lambda-generated classes in runtime-sensitive UI or patched-helper paths, and inspect/test affected entry points after helper placement changes.
- After adding new GUI helper classes, verify the live jar contains those classes and restart Starsector before testing. Starsector's script classloader can keep an older jar index for a running process; hot-copying a jar while the game is already open can still produce `NoClassDefFoundError` for new classes.
- Keep `tools\validate-live-gui-classes.ps1` current when helper ownership changes. It should check required current helpers, intentional nested helper interfaces, and forbidden stale helper classes, because stale or missing jar entries can hide migration regressions until runtime.
- The patched badge path must remain isolated from the popup. The badge helper should keep asking only for precomputed badge sprite state; it must not own stock logic, desired-stock logic, market scanning, buying, or GUI behavior.

## Count Bridge

Published properties:

- `wim.weapon.<weaponId>.player`
- `wim.weapon.<weaponId>.storage`
- `wim.fighter.<wingId>.player`
- `wim.fighter.<wingId>.storage`
- `wim.counts.ready`
- `wim.counts.updatedAt`

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
  - missing/invalid bridge state: `wim_total_err.png`.
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
robocopy data "C:\Games\Starsector\mods\Weapon Inventory Mod\data" /MIR
robocopy graphics "C:\Games\Starsector\mods\Weapon Inventory Mod\graphics" /MIR
robocopy jars "C:\Games\Starsector\mods\Weapon Inventory Mod\jars" /MIR
Copy-Item mod_info.json "C:\Games\Starsector\mods\Weapon Inventory Mod\mod_info.json" -Force
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
- Confirm `Sort` cycles through `Need`, `Name`, and `Price` without collapsing headings.
- Confirm weapon rows expand into Weapon Data and Sellers sections.
- Confirm mouse-wheel scrolling and clickable `^     ^     ^     ^     ^` / `v     v     v     v     v` indicators preserve state and do not appear when all rows fit.
- Confirm `+1`/dynamic buy-step works from top-level rows and specific seller rows, with credits/space failures blocked.
- Confirm no crash.
- Confirm commodities remain vanilla.
- Confirm weapon and fighter LPC stacks show one bottom-right badge.
- Confirm totals include player cargo plus all accessible storage.
- Confirm buy/sell/storage moves update while paused.
