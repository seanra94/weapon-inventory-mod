
## `PLANS.md`

```md
# PLANS.md

## Plan: Weapon ownership indicator for Starsector cargo weapon icons

Status: planned.

This is a multi-session feature because it involves UI hooks, cargo ownership aggregation, caching, and manual in-game verification across multiple cargo screens.

## Success criteria

The mod is successful when:

- weapon cargo icons show an ownership-count indicator;
- the count includes player fleet cargo and accessible storage;
- the display uses `0` through `99`, with `99+` for 100 or more;
- indicators appear in:
  - market trade UI;
  - player inventory/cargo UI;
  - storage UI, both player and storage panes if applicable;
  - salvage/loot UI if it uses the same cargo-stack rendering path;
- counts update while the UI remains open after buy/sell/transfer/salvage actions;
- non-weapon stacks do not show the indicator;
- the mod builds and can be deployed through the repo’s normal workflow.

## Constraints from AGENTS.md

Every implementation session must:

- start with `git status --short --branch`;
- inspect only the smallest relevant file set;
- use `rg --files` and `rg -n` before broad file reads;
- avoid generated files, lockfiles, logs, binaries, archives, vendored deps, and large assets unless directly relevant;
- prefer minimal local edits;
- run the smallest useful verification command;
- report:
  - files changed;
  - checks run;
  - deployment status;
  - whether `HANDOVER.md` changed;
  - whether `PLANS.md` changed;
  - remaining risk or next step.

## Phase 0: Initial repo orientation

Goal: identify existing project structure and build/deploy workflow.

Initial retrieval budget:

1. `git status --short --branch`
2. `rg --files -g '!*lock*' -g '!*.jar' -g '!*.zip' -g '!*.png' -g '!*.jpg' -g '!*.class'`
3. `rg -n "ModPlugin|modPlugin|CargoStack|Availability|rankIcon|getRankIcon|Listener|Submarkets|SUBMARKET_STORAGE|playerHasStorageAccess|build|deploy|package" README* HANDOVER.md PLANS.md build.gradle build.gradle.kts mod_info.json src data scripts 2>/dev/null`

Initial targeted reads, capped unless findings justify more:

- `mod_info.json`
- build file
- existing mod plugin
- nearest file matching cargo-stack listener/provider terms
- `README*` or existing deployment docs

Deliverable:

- concise orientation summary;
- update `HANDOVER.md` only if durable build/deploy/project facts were discovered.

## Phase 1: Prove the UI hook

Goal: determine the least invasive way to render something on a cargo-stack weapon icon.

Targeted searches:

```sh
rg -n "CargoStackAvailability|AvailabilityIcon|RankIcon|getRankIcon|CargoStackView|CargoStackAPI|addListener|ListenerManager" src data
```

## Current status

Phase 0/1 static marker hook is still not proven.

Manual test results so far:

1. Initial hook proof: no visible marker on weapon stacks.
2. Follow-up hook proof: entering a planet trade screen crashed with:
   `File access and reflection are not allowed to scripts. (java.lang.reflect.Field)`
3. Commit `3a8a998`: crash fixed, but still no visible marker.
4. Commit `15e0240`: diagnostic marker forced from icon/rank methods; no crash, but still no visible marker in tested weapon contexts.

## Immediate next step

Classify the failure using `starsector.log` diagnostic lines:

- no `WIM_DIAG register`: mod loading or deployment problem;
- register but no priority: tested UI is not querying this provider;
- priority but no icon/rank method calls: provider likely not selected, test literal priority `100`;
- icon/rank method calls plus sprite resolved but no visual marker: sprite visibility or render-path issue;
- icon/rank method calls plus sprite failure: fix settings/path/deployed graphics.

Ownership counting, storage scanning, generated number sprites, and tooltip changes remain blocked until a static marker visibly renders.

## Current status (latest)

Phase 0/1 static marker hook is still not proven.

Latest result:

- WIM priority was changed to literal `100`, matching Demand Indicator bytecode evidence.
- Manual retest still showed no WIM marker.
- Demand Indicators remained enabled and its commodity indicators were visible.

## Immediate next step (latest)

Classify the post-priority-100 log result.

If `WIM_DIAG priority` appears but `getRankIconName` / `getIconName` calls are absent, test provider-selection conflict by raising WIM diagnostic priority above `100`.

Also run a no-code control test with Demand Indicators disabled.

## Architecture decision (latest)

- Public hook path cannot provide a weapon icon badge overlay.
- Internal prototype path selected for proof only, with strict reversibility.

## Immediate next step (latest)

1. Keep commodity diagnostics disabled (no `CommodityIconProvider` registration).
2. Run reversible core patch proof:
   - patch only `CargoStackView.renderAtCenter(float,float,float)`;
   - inject one static call `CargoWeaponMarkerHook.render(float)` in `WEAPONS` branch;
   - pass only alpha;
   - no ownership counts, no storage scan, no number sprites, no wings.
3. Manual verify:
   - no crash on game start/trade;
   - no commodity red-box diagnostics;
   - fixed marker visible on weapon cargo icons.

## Current status (class visibility)

The bytecode prototype reached the weapon cargo render path but failed class visibility.

Observed crash:

- entering market trade called patched `CargoStackView.renderAtCenter(...)`;
- JVM could not resolve `weaponinventorymod.internal.CargoWeaponMarkerHook`;
- result: `NoClassDefFoundError`.

## Immediate next step (class visibility fix)

- restore original `starfarer_obf.jar`;
- rebuild WIM classes;
- patch `CargoStackView.renderAtCenter(FFF)V`;
- also embed `weaponinventorymod/internal/CargoWeaponMarkerHook.class` into patched `starfarer_obf.jar`;
- verify both injected call and hook class are present;
- redeploy WIM payload;
- manually retest market trade.

Ownership counting, storage scanning, number sprites, and wing support remain blocked until fixed marker renders without crashing.

## Current status (render visibility)

Class visibility crash is fixed, but marker visibility is still under diagnostic.

Known facts:

- patched hook is invoked from `CargoStackView.renderAtCenter(...)`;
- previous sprite-based hook path failed because `Global.getSettings()` was `null` in hook context;
- no crash now from class loading.

## Immediate next step (render diagnostics)

- keep primitive hook boundary (`render(float alpha)`);
- log once at first line of hook entry;
- draw large raw-GL diagnostic squares independent of sprite/settings lookup;
- if entry logs appear but squares are still invisible, repatch hook boundary to `render(float x, float y, float alpha)` and draw using explicit coordinates.

## Current status (raw GL failure)

The hook reachability is proven, but raw hook-side GL rendering failed.

Known facts:

- `WIM_WEAPON_HOOK reached alpha=1.0` confirmed call entry;
- hook-side `GL11` call failed with `No OpenGL context found in the current thread`;
- no visible marker rendered from external hook call path.

## Immediate next step (internal duplicate draw diagnostic)

- remove injected `CargoWeaponMarkerHook.render(...)` call from patched `CargoStackView`;
- inject a second call to existing internal `weaponIcon` draw method in `WEAPONS` branch;
- use visible x-offset for duplicate icon proof;
- keep patch deterministic/reversible and limited to `CargoStackView.renderAtCenter(FFF)V`;
- keep ownership counts, storage scanning, number sprites, tooltip work, and fighter wings out of scope.

## Current status (marker draw replacement)

- Duplicate-weapon diagnostic has been replaced by a marker-style sprite draw injection in the WEAPONS branch.
- Patch now uses `com/fs/graphics/Sprite` draw flow with marker asset path `graphics/ui/weapon_inventory_test_marker.png`.
- Legacy external hook-call path remains disabled.

## Immediate next step (manual verification)

- launch game and open market trade;
- confirm no crash;
- confirm commodities remain vanilla (no WIM red boxes);
- confirm duplicate-weapon icon is gone;
- confirm fixed marker appears on weapon icons.

## Current status (placement stabilization pass)

- Marker rendering is now injected near the rank-marker stage with explicit WEAPONS gating.
- Placement math now uses slot/cell dimensions from `getPosition().getWidth()/getHeight()`, not weapon-sprite dimensions.
- Duplicate-weapon diagnostic draw and external hook-call paths remain blocked/refused by patcher safeguards.

## Immediate next step (manual placement verification)

- manually verify marker consistency across mixed weapon sprite shapes/sizes in market trade;
- if small drift remains, tune only fixed padding constants (`+5` / `-5`) while keeping slot-based anchors;
- keep ownership counting/storage/99+/wings out of scope until placement is visually stable.

## Current status (post-70e4c64 debug)

- No crash; commodities remain vanilla.
- Marker became invisible after `70e4c64` because injection landed in a late rank-marker section that WEAPONS flow does not reach.
- Patch has been moved back to the reachable WEAPONS branch end, keeping slot-based coordinate math.

## Immediate next step

- manually verify marker is visible again on weapon stacks (size 1 and size >1);
- verify placement consistency across weapon sprite shapes;
- keep count rendering blocked until marker visibility and placement are stable.

## Current status (visibility recovery + count feasibility)

- `b9fe9ec` marker invisibility was traced to insertion at the non-WEAPONS jump label; WEAPONS flow skipped that block.
- Marker injection is now back in reachable WEAPONS flow.
- Minimal count feasibility proof is now active:
  - render marker only when player-fleet cargo `getNumWeapons(weaponId) > 0`.

## Immediate next step

- manual verify in market trade:
  - no crash;
  - commodities vanilla;
  - marker visible for owned (`>0`) weapons and absent for unowned (`0`) weapons;
  - stack-size-1 and stack-size->1 behavior;
  - placement quality noted separately.
- keep storage aggregation, `99+`, wing support, tooltip changes, and final styling out of scope.
