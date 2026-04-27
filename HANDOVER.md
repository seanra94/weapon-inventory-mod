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

- Commit `3a8a998` fixed the trade-screen crash caused by forbidden runtime member probing.
- Manual retest after `3a8a998`: no crash, but still no visible test marker on weapon stacks.
- Current unresolved issue: the mod has not proven that weapon cargo stacks reach `CommodityIconProvider.getIconName(CargoStackAPI)` with `stack.isWeaponStack() == true`.
- Do not assume Demand Indicator’s commodity overlay path applies to weapons. Demand Indicator is useful evidence for commodity cargo icons, but weapon cargo icons may use a different render path or stack representation.
- Next hook-proof pass is diagnostic-only:
  - log provider registration;
  - log `getHandlingPriority(...)` params class;
  - log capped calls to `getIconName(...)` and `getRankIconName(...)`;
  - temporarily force the test marker from icon/rank methods for all non-null stacks to identify which render path is active;
  - remove or narrow the diagnostic behavior after the render path is identified.
- Durable rule remains: no Java runtime member probing in Starsector script code.
- Confirmed local deploy target remains `C:\Games\Starsector\mods\Weapon Inventory Mod`.
- Confirmed deploy payload for test updates: `mod_info.json`, `data`, `graphics`, `jars`.
- Confirmed deploy command pattern used here: `robocopy <src> <dst> <selection> /E`.

### Latest diagnostic classification

- Commit `15e0240` added diagnostic hook logging and temporarily forced the test marker from both `CommodityIconProvider.getIconName(...)` and `getRankIconName(...)` for all non-null stacks.
- Manual retest after `15e0240`: no crash, but still no visible marker in tested weapon contexts.
- Visual result alone is not enough to diagnose the hook; classification must use `starsector.log` lines beginning with `WIM_DIAG`.
- Latest classification after `15e0240`: Branch C (register + priority logs present, but no `getIconName(...)` or `getRankIconName(...)` call logs).
- Branch C conclusion: provider appears queried but not selected for icon methods in tested context.
- Demand Indicator bytecode evidence remains:
  - `getHandlingPriority(Object)` returns literal `100`;
  - `getIconName(CargoStackAPI)` returns `null`;
  - indicator logic is in `getRankIconName(CargoStackAPI)`;
  - provider registration happens from plugin `onGameLoad(...) -> register()`.
- Diagnostic priority is now set to literal `100`, aligned with Demand Indicator bytecode evidence.

## In-game verification notes

- Phase 0/1 initial hook proof failed manual in-game testing: no UI marker was visible on weapon stacks in any checked context.
- Follow-up hook pass changed sprite registration to top-level `graphics` and moved the test marker to `CommodityIconProvider.getIconName(...)`.
- That follow-up crashed when entering a planet trade screen with:
  - `Fatal: File access and reflection are not allowed to scripts. (java.lang.reflect.Field)`
- Durable rule: do not use Java reflection, `java.lang.reflect.Field`, `getDeclaredField(...)`, `getField(...)`, or `setAccessible(...)` in Starsector runtime script code.
- If `CommodityIconProvider.getHandlingPriority(Object params)` receives an unknown wrapper object, do not inspect internal members dynamically. Prefer public/type-safe checks and rely on `getIconName(CargoStackAPI)` to gate weapon-only rendering.

## Hook proof status update (latest)

- After diagnostic priority was set to literal `100`, manual retest still showed no WIM marker.
- Demand Indicators remained enabled and its commodity indicators were visible.
- Current leading hypothesis: provider-selection conflict. Demand Indicator's provider also returns priority `100`, so WIM may still lose selection by tie/order and never receive `getRankIconName(...)`.
- Next diagnostic branch:
  - inspect post-priority-100 `WIM_DIAG` lines;
  - if priority is logged but rank/icon calls are absent, raise WIM diagnostic priority above `100` to test provider selection;
  - also run a manual no-code control test with Demand Indicators disabled.

## Weapon icon overlay architecture decision (latest)

- Public-safe weapon/wing icon overlay hook was not found.
- `CommodityIconProvider` affects commodity/resource cargo icons only and is not usable for weapon icon overlays.
- Weapon cargo icons are rendered through a private/internal `CargoStackView.weaponIcon` path inside `CargoStackView.renderAtCenter(float,float,float)`.
- Fighter wings use a separate `FIGHTER_CHIP` path.
- Sprite/spec mutation is rejected because it would likely affect global weapon visuals, not cargo-only visuals.
- Reflection/member probing is not viable because Starsector blocks it at runtime.
- The commodity diagnostic provider path has been disabled in runtime:
  - `WeaponInventoryModPlugin` no longer registers `WeaponCargoTestIconProvider`;
  - `WeaponCargoTestIconProvider` source was removed.
- Risky internal prototype path introduced as a local reversible patch:
  - target class: `com.fs.starfarer.campaign.ui.trade.CargoStackView`;
  - target method: `renderAtCenter(float,float,float)` descriptor `(FFF)V`;
  - injected call: `weaponinventorymod.internal.CargoWeaponMarkerHook.render(float)`;
  - injected argument: existing method arg `alpha` (`fload_3`) only.
- Local patch tooling:
  - script: `tools/cargo-stack-view-patcher.ps1`;
  - patch command: `powershell -ExecutionPolicy Bypass -File .\tools\cargo-stack-view-patcher.ps1 -Mode Patch`;
  - restore command: `powershell -ExecutionPolicy Bypass -File .\tools\cargo-stack-view-patcher.ps1 -Mode Restore`;
  - backup path: `C:\Games\Starsector\starsector-core\starfarer_obf.jar.wim_backup`.
- Safeguards enforced by patcher:
  - backup before patch if absent;
  - refuse if target class/method missing;
  - refuse if deterministic `WEAPONS` insertion point is not found;
  - refuse if already patched;
  - patch one class/method only.

## Bytecode prototype classloading result

- First bytecode-patch prototype reached `CargoStackView.renderAtCenter(...)` but crashed entering market trade with:
  - `java.lang.NoClassDefFoundError: weaponinventorymod/internal/CargoWeaponMarkerHook`
  - caused by `ClassNotFoundException: weaponinventorymod.internal.CargoWeaponMarkerHook`
- Diagnosis: patched core class `CargoStackView` is loaded from `starfarer_obf.jar` and cannot assume classes in the WIM mod jar are visible.
- Durable patcher invariant: a patched `starfarer_obf.jar` must contain both:
  - the patched `CargoStackView` class with the injected call;
  - `weaponinventorymod/internal/CargoWeaponMarkerHook.class`.
- Atomic patch safeguards now enforced:
  - refuse patch if hook class source is missing;
  - refuse patch if method already patched;
  - refuse patch on inconsistent state unless explicit `repair` mode;
  - verify after patch that both injected call and embedded hook class exist.
- Hook boundary remains primitive-only:
  - `public static void render(float alpha)`;
  - no runtime reflection;
  - no dependency on mod plugin/helper classes;
  - no ownership counting or cargo-stack access in static-marker proof.

## Bytecode weapon-marker prototype status (visibility diagnostics)

- Classloader/package visibility is now fixed (embedded hook class), and the hook is reached from patched `CargoStackView.renderAtCenter(...)`.
- Latest log evidence in `starsector-core/starsector.log` shows:
  - `WIM_HOOK sprite lookup failed for ui.weapon_inventory_test_marker`
  - `NullPointerException` because `Global.getSettings()` returned `null` inside `CargoWeaponMarkerHook`.
- Durable lesson: core-jar-invoked hook code should not rely on `Global.getSettings()` being initialized/available in this render context.
- Diagnostic hook now logs once at method entry and draws raw GL colored quads independent of sprite/settings lookup to prove render-path visibility.
- Patch descriptor currently remains `render:(F)V`; if hook-entry logs appear but raw quads stay invisible, next diagnostic step is to repatch to `render:(FFF)V` and pass primitive `x,y,alpha`.

## Bytecode weapon-marker prototype status (current)

- The embedded hook class is loadable and the injected weapon-branch call was reached.
- Manual/log result for raw-GL hook pass:
  - `WIM_WEAPON_HOOK reached alpha=1.0`
  - raw `GL11` drawing failed with `No OpenGL context found in the current thread`.
- Durable lesson: do not draw with raw LWJGL/OpenGL from the external embedded hook class in this path.
- Current diagnostic approach:
  - remove injected `CargoWeaponMarkerHook.render(...)` call from `CargoStackView`;
  - inject a direct duplicate call to internal `weaponIcon` draw method with visible x-offset;
  - keep patch to one method/class only and reversible via restore.
- Current injected duplicate draw uses:
  - field: `CargoStackView.weaponIcon` (owner `com/fs/starfarer/campaign/ui/trade/CargoStackView`);
  - draw method owner: obfuscated weapon icon class in `com/fs/starfarer/ui/B/...`;
  - draw descriptor: `(FFFF)V`;
  - args: `x=18.0f`, `y=-local10`, `angle=local13`, `alpha=arg3`.
- `CargoWeaponMarkerHook` class remains in source as harmless legacy no-op and is no longer called by patched core bytecode.

## Bytecode weapon-marker prototype status (marker draw patch)

- Duplicate weapon-icon diagnostic draw has been removed from the patcher path.
- Current patch injects marker drawing directly in `CargoStackView.renderAtCenter(FFF)V` WEAPONS branch using `com/fs/graphics/Sprite`, not `weaponIcon` duplicate draw and not `CargoWeaponMarkerHook`.
- Injected marker path:
  - sprite path literal: `graphics/ui/weapon_inventory_test_marker.png`
  - draw flow: `new Sprite(path) -> setNormalBlend() -> setAlphaMult(alpha) -> render(x, y)`
  - coordinates mirror RESOURCES rank-marker layout math:
    - `x = -width/2 + 5`
    - `y = height/2 - markerHeight - 5`
- Patcher safeguards now refuse:
  - legacy hook-call patch present;
  - old duplicate-weapon diagnostic patch present;
  - marker patch already present.
