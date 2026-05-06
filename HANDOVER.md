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
- Popup default scope:
  - `data/config/weapon_inventory_stock.json` now defaults to `ALL_TRACKED`, so the popup starts from all enabled weapon specs. `Owned Or For Sale` remains available as a narrower mode.
- Popup purchase flow:
  - top-level row buttons buy from cheapest eligible current-market seller stock;
  - expanded Seller rows buy from a specific submarket;
  - buys check player credits and cargo space before moving stock;
  - after a successful buy, the popup asks the vanilla cargo core UI to close/reopen because direct cargo mutation while the trade grid is open can leave stale slot views behind;
  - this is intentionally isolated in `StockPurchaseService` for future transaction-side-effect hardening.
- Popup category layout:
  - stock categories start collapsed;
  - headings are flat full-width peer rows, not nested checkboxes;
  - weapon rows, nested section rows, seller rows, and scroll indicators are all explicit row descriptors rather than ad hoc tooltip paragraphs.
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
