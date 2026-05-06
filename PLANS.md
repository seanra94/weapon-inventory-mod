# Weapon Inventory Mod Plans

## Current Status

- Working architecture:
  - deterministic `CargoStackView.renderAtCenter(FFF)V` bytecode patch for in-cell badge rendering;
  - normal mod-side paused updater for counts;
  - JVM `System` property bridge between updater and embedded helper;
  - one precomposed badge sprite per weapon or fighter LPC stack.
- Clean frontend work has started:
  - `F8` opens a normal read-only Weapon Stock Review popup from active market/storage interaction dialogs;
  - popup data comes from shared stock snapshot services, not the bytecode badge path;
  - rows show `owned / currently purchasable at this market`.
  - popup has config-backed desired stock defaults, display mode, storage inclusion, black-market inclusion, and per-weapon override scaffolding.
- Current visual baseline:
  - bottom-right placement;
  - stable pre-scale render frame;
  - no late over-icon rendering;
  - no layered text/background rendering.
- Current config baseline:
  - LunaLib dependency is present;
  - Luna setting currently controls updater interval only.

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

## Active Manual Validation

- Open market trade and verify:
  - no crash;
  - `F8` opens the Weapon Stock Review popup;
  - `Esc` and `Close` dismiss the popup;
  - category headings expand/collapse and preserve current state across refresh;
  - Mode cycles through `Owned or For Sale`, `Currently For Sale`, `Owned Only`, and `All Tracked`;
  - Market Storage and Black Market toggles update counts/categories;
  - no-weapons and many-weapons markets remain responsive;
  - commodities remain vanilla;
  - weapon badges still render bottom-right;
  - fighter LPC badges still render bottom-right;
  - totals still aggregate player cargo plus all accessible storage;
  - buy/sell/storage moves update while paused;
  - no old diagnostic squares or static marker assets appear.

## High-Value Future Work

- Remove or reduce capped runtime diagnostic logs once the latest cleanup is manually validated.
- Consider Luna settings for thresholds only if the implementation can stay precomposed and asset-backed without runtime tint/layering.
- Add a small validation command that checks patched `CargoStackView` helper-call counts and renders a concise report, instead of relying on ad hoc `javap` inspection.
- Consider publishing packaging notes for forum users that clearly explain the core-jar patch/restore requirement.

## Avoid Unless Reopened Deliberately

- Do not retry late over-icon rendering casually; it caused invisibility/blur regressions.
- Do not use layered background plus text badge rendering.
- Do not put campaign-state or LunaLib calls into `WeaponInventoryBadgeHelper`.
- Do not reintroduce runtime reflection or raw GL rendering for the badge path.
