
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

## Immediate next step

Run a diagnostic hook pass before ownership counting:

- add capped logs for provider registration, handling-priority params, `getIconName(...)`, and `getRankIconName(...)`;
- temporarily force the test marker from icon/rank methods for all non-null cargo stacks;
- inspect Demand Indicator bytecode only if cheap and only as behavioral evidence;
- rebuild, redeploy, and manually retest;
- use the visual result plus `starsector.log` to decide whether `CommodityIconProvider` can support weapon icons.

Ownership counting, storage scanning, generated number sprites, and tooltip changes remain blocked until a static marker visibly renders.
