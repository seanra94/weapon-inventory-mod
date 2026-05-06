# Weapon Inventory Mod Handover

## Current State

- The mod renders ownership-count badges directly inside vanilla market/trade cargo cells for weapon stacks and fighter LPC stacks.
- Exact in-cell rendering is implemented by deterministically patching `com/fs/starfarer/campaign/ui/trade/CargoStackView.renderAtCenter(FFF)V` in `starfarer_obf.jar`.
- A clean normal-mod frontend is now being added as the primary product: `F8` opens a Weapon Stock Review popup from an active market/storage interaction dialog.
- The popup is independent of the `CargoStackView` patcher. The clean UI should keep working if the patcher is removed.
- Popup configuration lives in `data/config/weapon_inventory_stock.json`:
  - default display mode;
  - include/exclude current market storage;
  - include/exclude black market stock;
  - desired stock defaults by weapon size;
  - per-weapon desired/ignored overrides.
- Popup redraw rule:
  - The clean popup now renders through an explicit custom-panel shell, not one long tooltip row pile.
  - `StockReviewRenderer` builds fixed-height custom row panels from `StockReviewListModel`; `StockReviewPanelPlugin` removes/recreates one root content panel for state changes.
  - State-changing category/weapon/section/mode/sort/filter actions should rebuild the custom content panel in place and preserve `StockReviewState`, including list scroll offset.
- Popup button rule:
  - Buttons use real Starsector button labels plus a small `StockReviewButtonBinding` registry as a polling fallback. In runtime, nested custom-panel controls did not reliably arrive through `buttonPressed(...)` alone.
  - Keep row/button actions as explicit `StockReviewAction` ids; do not return to inferred checkbox state as the source of truth.
  - Clickable rows/buttons now use blank Starsector button text with WIM-rendered labels layered separately, so hover/base colors stay ACG-like while visible text remains white/gray under WIM control.
- Popup default scope:
  - `data/config/weapon_inventory_stock.json` now defaults to `ALL_TRACKED`, so the popup starts from all enabled weapon specs. `Owned Or For Sale` remains available as a narrower mode.
- Popup purchase flow:
  - top-level row buttons queue a pending purchase from cheapest eligible current-market seller stock;
  - expanded Seller rows queue a pending purchase from that specific submarket;
  - queued purchases are held in the popup until the user opens `Review Purchase`;
  - the review screen lists each weapon purchase, per-line cost, total cost, and current credits, then uses `Confirm Purchase` / `Go Back`;
  - only `Confirm Purchase` mutates cargo, checks player credits and cargo space, and rebuilds the popup snapshot afterward;
  - this avoids the awkward immediate recategorization where buying one `No stock` weapon moves it out of that category before the user finishes shopping;
  - forced vanilla cargo core close/reopen is kept only as a fallback because direct cargo mutation while the trade grid is open can leave stale slot views behind;
  - this is intentionally isolated in `StockPurchaseService` for future transaction-side-effect hardening.
- Popup category layout:
  - stock categories start collapsed;
  - headings are flat full-width peer rows, not nested checkboxes;
  - weapon rows, nested section rows, seller rows, and scroll indicators are all explicit row descriptors rather than ad hoc tooltip paragraphs.
- Popup visual rules:
  - WIM intentionally mirrors the accepted ACG palette in `StockReviewStyle`: red/cancel for No stock, yellow/load for Insufficient and Buy buttons, green/confirm for Sufficient, dark gray collapsible headings, black neutral action rows, and gray text only for disabled controls.
  - Use white/default-font text for ordinary popup text and buttons unless a specific disabled/locked convention applies.
  - The three top stock category headings use their red/yellow/green fills. Nested toggle headings such as `Weapon data` and `Sellers` use the ACG dark-gray collapsible heading fill.
  - WIM-owned row fills sit behind Starsector buttons while button backgrounds are dimmed, intentionally recreating ACG's inner dimmed rectangle with brighter outer row fill.
  - Weapon rows, seller rows, review rows, and button hitboxes use white grid borders. Nested `Weapon data` / `Sellers` heading buttons start after the indent so the left indent remains black.
- Popup list filtering:
  - Category counts and weapon rows are filtered to records with at least one currently buyable unit at the open market. The popup is for shopping, not for showing unavailable desired weapons.
  - Weapon row count text is `owned / buyable here`, not `owned / visible including locked stock`. Locked seller rows may still appear inside the expanded Sellers section for context.
- Popup availability:
  - `F8` is now gated to `SectorAPI.getCurrentlyOpenMarket()` plus at least one weapon currently buyable under the current black-market setting. It should not open from looting, non-trade planet contexts, or markets with only locked/unbuyable weapon stock.
- Purchase refresh:
  - The current preferred buy path does not force-close/reopen the vanilla cargo core after purchase; it mutates cargo, then rebuilds the popup snapshot in place. The old forced core refresh remains behind `StockReviewStyle.REFRESH_VANILLA_CORE_AFTER_PURCHASE` in case vanilla trade-grid stale-slot corruption still reproduces.
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
- Starsector area-checkbox colors are counterintuitive:
  - `base` behaves like hover/glow;
  - `bg` behaves like checked fill/border;
  - built-in label coloring is limited, so WIM should keep using controlled row text instead of relying on checkbox labels for complex rows.
- Starsector dims idle button interiors heavily. Raw RGB values can look much darker in game, while hover/glow is closer to raw RGB. For WIM, keep hover/glow equal to the idle/base color unless a runtime-tested exception is deliberately accepted.
- Avoid bare `addAreaCheckbox(...)` visuals for action rows. If WIM uses area checkboxes for a future row type, give the row an owned background/fill so idle colors do not degrade into only a border or ring.
- Use the imported ACG palette consistently:
  - No stock category rows: cancel red;
  - Insufficient and Buy rows/buttons: load yellow;
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
robocopy data "C:\Games\Starsector\mods\Weapon Inventory Mod\data" /MIR
robocopy graphics "C:\Games\Starsector\mods\Weapon Inventory Mod\graphics" /MIR
robocopy jars "C:\Games\Starsector\mods\Weapon Inventory Mod\jars" /MIR
Copy-Item mod_info.json "C:\Games\Starsector\mods\Weapon Inventory Mod\mod_info.json" -Force
powershell -ExecutionPolicy Bypass -File .\tools\validate-total-badges.ps1
```

Manual validation:

- Launch/load save.
- Open a market trade screen.
- Press `F8` to open Weapon Stock Review.
- Confirm the popup groups weapons under No stock, Insufficient stock, and Sufficient stock.
- Confirm row counts are `owned / currently purchasable at this market`.
- Confirm `Mode`, `Market Storage`, and `Black Market` buttons rebuild the snapshot without layered stale text and without closing/reopening the popup.
- Confirm `Sort` cycles ordering without collapsing headings.
- Confirm weapon rows expand into Weapon Data and Sellers sections.
- Confirm mouse-wheel scrolling and clickable `^     ^     ^     ^     ^` / `v     v     v     v     v` indicators preserve state and do not appear when all rows fit.
- Confirm `Buy 1`/`Buy 10` works from top-level rows and specific seller rows, with credits/space failures blocked.
- Confirm no crash.
- Confirm commodities remain vanilla.
- Confirm weapon and fighter LPC stacks show one bottom-right badge.
- Confirm totals include player cargo plus all accessible storage.
- Confirm buy/sell/storage moves update while paused.
