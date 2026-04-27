# HANDOVER.md

## Project purpose

This Starsector mod adds a small ownership-count indicator to weapon cargo icons.

When the player views a weapon stack in trade, storage, inventory, salvage, or other cargo-style screens, the weapon icon should show how many copies of that weapon the player owns across:

- player fleet cargo;
- accessible storage submarkets;
- abandoned/free storage locations, if Starsector exposes them separately from normal market storage;
- any other durable player-owned storage source discovered during implementation.

The indicator has room for up to three characters:

- `0` through `99` for exact counts;
- `99+` for counts of 100 or more.

Example target behavior:

- If a market sells a Light Machine Gun and the player has 3 in fleet cargo, 2 in one storage, and 5 in another storage, the indicator shows `10`.
- If the player has 500 copies across all storage locations, the indicator shows `99+`.

## Core user goals

- Show weapon ownership counts directly on weapon icons.
- Show the indicator anywhere weapon cargo stacks are visible, not just on market trade screens.
- Update quickly while the UI remains open, including after buying, selling, transferring, or salvaging weapons.
- Keep the implementation small, maintainable, and low-risk.
- Prefer targeted source inspection, small diffs, and focused verification.

## Important baseline references

### Demand Indicator baseline

Demand Indicator is the best first baseline for icon placement and cargo-stack rendering hooks.

Known durable facts from public inspection:

- It is a Starsector `0.98a` mod.
- It adds indicators to cargo areas for commodity shortage/excess information.
- It loads a compiled jar: `jars/demandindicators.jar`.
- Its mod plugin is declared as `demandindicators.plugins.ModPlugin`.
- Its settings define UI sprite keys for indicator icons under `graphics/ui/...`.
- A public crash trace shows a class named `demandindicators.listener.CargoStackAvailabilityIconProvider` with a method named `getRankIconName()` being called from `CargoStackView.renderAtCenter`.

Working inference:

- The first hook to inspect in this project is the Starsector cargo-stack availability/rank-icon provider path used by `CargoStackView`.
- Do not assume this hook can render dynamic text. It may only return a sprite name.
- If the hook only accepts static sprite keys, the mod may need either:
  - pre-generated sprites for `0` through `99+`; or
  - a more invasive render hook, which should be avoided unless necessary.

Licensing caution:

- Do not copy Demand Indicator code unless the license permits it. Use it primarily as behavior and architecture evidence.

### Stellar Networks baseline

Stellar Networks is the best first baseline for discovering all player-accessible storage locations.

Known durable facts from public inspection:

- It implements a Storage board.
- It scans player-accessible storage via helper logic.
- Its helper pattern includes:
  - obtaining abandoned/free storage sources;
  - iterating economy markets;
  - checking storage access;
  - reading `Submarkets.SUBMARKET_STORAGE`;
  - aggregating cargo stacks.

Licensing caution:

- Stellar Networks is GPL-3.0 licensed. Reimplement the storage-discovery idea against Starsector APIs instead of copying source unless this mod will be GPL-compatible.

## Likely implementation architecture

### 1. Mod plugin

Expected responsibilities:

- Register the cargo-stack indicator provider/listener during game load.
- Keep registration idempotent.
- Avoid duplicate listeners after reloads.
- Load settings if needed.

Likely file:

- `src/.../plugins/ModPlugin.java` or equivalent existing project plugin.

Exact class names depend on the project skeleton.

### 2. Cargo-stack indicator provider

Expected responsibilities:

- Receive a `CargoStackAPI` or equivalent stack object from the UI hook.
- Return no indicator for non-weapon stacks.
- Resolve weapon id from the stack.
- Ask the ownership-count service for the current total count.
- Format the count as `0` to `99` or `99+`.
- Return the correct sprite key or draw text, depending on what the hook supports.

Likely file:

- `src/.../ui/WeaponOwnershipCargoIconProvider.java`
- or a package matching the hook discovered from Demand Indicator.

Critical unknown:

- Whether Starsector’s cargo-stack icon hook can render dynamic text.
- If it only returns sprite keys, implement sprite-per-label first because it is safer than reflection into vanilla UI classes.

### 3. Weapon ownership counter

Expected responsibilities:

- Count copies of a weapon id across all player-owned sources.
- Keep the count independent from the currently viewed stack’s source.
- Avoid expensive full rescans on every rendered stack.

Suggested class:

- `WeaponOwnershipCounter`
- or `WeaponOwnershipIndex`

Suggested public API:

```java
public int getOwnedCount(String weaponId);
public String formatCount(int count); // "0".."99", "99+"
public void invalidate();
```

## Implemented baseline (Phase 0/1)

- Existing repo state at start: no source/build files; only planning docs.
- Hook path selected: `com.fs.starfarer.api.campaign.listeners.CommodityIconProvider`.
- Registration method: add a provider instance to `Global.getSector().getGenericPlugins()` during `BaseModPlugin.onGameLoad(...)`, with idempotency check via `getPluginsOfClass(...)`.
- Implemented classes:
  - `src/weaponinventorymod/plugins/WeaponInventoryModPlugin.java`
  - `src/weaponinventorymod/listener/WeaponCargoTestIconProvider.java`
- Current behavior: weapon cargo stacks return a fixed test rank icon; non-weapon stacks fall back to vanilla/default rank icon behavior.
- Sprite wiring:
  - key: `ui.weapon_inventory_test_marker`
  - file: `graphics/ui/weapon_inventory_test_marker.png`
  - config: `data/config/settings.json`
- Build workflow:
  - command: `powershell -ExecutionPolicy Bypass -File .\build.ps1`
  - output jar: `jars/weapon-inventory-mod.jar`
  - compile classpath must include both `starfarer.api.jar` and `log4j-1.2.9.jar` for `Global.getLogger(...)` usage.
- Deployment workflow in this environment:
  - deploy payload (`mod_info.json`, `data`, `graphics`, `jars`) to `C:\Games\Starsector\mods\Weapon Inventory Mod` after rebuild.

## Hook proof status update

- First in-game hook proof failed: no visible test marker appeared on weapon stacks in market/storage/inventory/salvage contexts.
- Updated hook proof now uses `getIconName(CargoStackAPI)` for weapon-only icon override and preserves vanilla rank behavior through `getRankIconName(CargoStackAPI)`.
- Sprite registration is now expected to be defined in `data/config/settings.json` under top-level `graphics` (category `ui`), not top-level `sprites`.
- Provider registration now logs once, and sprite lookup failure now logs once, to make validation in `starsector.log` straightforward.
- `getHandlingPriority(Object params)` should treat unknown params as no-op (`-1`); API evidence shows vanilla may pass a wrapper object carrying `stack` rather than a bare `CargoStackAPI`.
- Confirmed local deploy target remains `C:\Games\Starsector\mods\Weapon Inventory Mod`.
- Confirmed deploy payload for test updates: `mod_info.json`, `data`, `graphics`, `jars`.
- Confirmed deploy command pattern used here: `robocopy <src> <dst> <selection> /E`.

## In-game verification notes

- Phase 0/1 initial hook proof failed manual in-game testing: no UI marker was visible on weapon stacks in any checked context.
- Follow-up hook pass changed sprite registration to top-level `graphics` and moved the test marker to `CommodityIconProvider.getIconName(...)`.
- That follow-up crashed when entering a planet trade screen with:
  - `Fatal: File access and reflection are not allowed to scripts. (java.lang.reflect.Field)`
- Durable rule: do not use Java reflection, `java.lang.reflect.Field`, `getDeclaredField(...)`, `getField(...)`, or `setAccessible(...)` in Starsector runtime script code.
- If `CommodityIconProvider.getHandlingPriority(Object params)` receives an unknown wrapper object, do not inspect internal members dynamically. Prefer public/type-safe checks and rely on `getIconName(CargoStackAPI)` to gate weapon-only rendering.
