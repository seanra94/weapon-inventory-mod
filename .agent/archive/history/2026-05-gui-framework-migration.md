# GUI framework migration history

Status: historical
Scope: Weapons Procurement stock-review GUI and shared `WimGui*` extraction
Last verified: 2026-05-12, summarized from the pre-debloat `PLANS.md`
Read when: investigating why the popup uses the current custom-panel/list architecture or why a `WimGui*` helper exists
Do not read for: ordinary feature work where current ownership is already clear from `HANDOVER.md`
Related files: `src/weaponsprocurement/gui`, `src/weaponsprocurement/gui/wimgui`, `tools/validate-live-gui-classes.ps1`
Search tags: `WimGui`, `ACG`, `StockReviewRenderer`, `StockReviewPanelPlugin`, `NoClassDefFoundError`, `modal list`, `button poller`

## Summary

- The stock review popup moved from tooltip-row rendering to an explicit custom-panel/list foundation.
- GUI behavior was deliberately extracted into reusable `WimGui*` helpers after repeated Starsector UI fragility.
- Button handling kept explicit `StockReviewAction` ids and added event-gated polling because nested custom-panel buttons were unreliable through `buttonPressed(...)` alone.
- Scroll/list/modal/text/layout behavior moved to shared helpers so future WP screens do not copy stock-review renderer math.
- Runtime classloader issues drove explicit helper classes and live-jar class validation.
- The ACG palette and modal/list shape were imported as shared style conventions.
- The end state is a stock-review panel that orchestrates lifecycle, with focused renderers/controllers owning UI, trade, mode, and execution concerns.

## Index

- `Popup foundation`: why the current custom-panel/list shape exists.
- `Shared helper extraction`: major `WimGui*` ownership migrations.
- `Classloader validation`: why live-jar checks and explicit classes matter.
- `Visual conventions`: ACG-derived palette and modal/list conventions.

## Details

## Popup foundation

The old tooltip-row popup was replaced by a custom-panel/list structure:

- `StockReviewListModel` builds main section/category row descriptors.
- `StockReviewReviewListModel` builds Review Trades row descriptors.
- `StockReviewRenderer` composes shell/header/footer/body while shared list helpers render rows.
- `StockReviewPanelPlugin` rebuilds one custom content panel in place for non-purchase actions.
- `StockReviewExpansionState`, `StockReviewFilterState`, and `StockReviewSourceState` moved focused UI state out of the broader `StockReviewState`.

This work superseded an earlier mitigation where the dialog was dismissed and reopened to avoid redraw layering.

## Shared helper extraction

The migration extracted reusable helpers for:

- button bindings and event-gated polling: `WimGuiButtonBinding`, `WimGuiButtonPoller`;
- declarative button rows: `WimGuiButtonSpec`, `WimGuiButtonSpecs`, `WimGuiSemanticButtonFactory`;
- modal footer/action/header placement: `WimGuiModalFooter`, `WimGuiModalActionRow`, `WimGuiModalHeader`;
- row and cell rendering: `WimGuiRowCell`, `WimGuiListRow`, `WimGuiListRowRenderer`;
- modal list rendering: `WimGuiModalListSpec`, `WimGuiModalListRenderer`, `WimGuiModalListRenderResult`;
- scroll and list state: `WimGuiScroll`, `WimGuiScrollSlice`, `WimGuiScrollableListState`, `WimGuiListBounds`;
- text fitting/wrapping: `WimGuiText`, `WimGuiTextLayout`;
- dialog host/opening/tracking: `WimGuiCampaignDialogHost`, `WimGuiDialogOpener`, `WimGuiDialogTracker`, `WimGuiPendingDialog`;
- modal lifecycle and input: `WimGuiModalPanelPlugin`, `WimGuiModalInput`, `WimGuiInputResult`;
- reusable paint/style: `WimGuiPanelPlugin`, `WimGuiStyle`.

The old plan explicitly said future WP screens should start from these helpers rather than copying stock-review renderer code.

## Classloader validation

Runtime testing produced `NoClassDefFoundError` after GUI helper extraction, exposing stale live-jar/classloader risk. The response was:

- add `tools/validate-live-gui-classes.ps1`;
- extract reusable class-list checks to `tools/validate-jar-classes.ps1`;
- reject stale removed GUI helper classes;
- validate intentional nested helper interfaces;
- prefer explicit classes over anonymous/local/lambda helpers in runtime-sensitive GUI paths.

`WeaponStockSnapshotBuilder$CostComparator` was temporarily restored as a compatibility shim after runtime sorting showed Starsector could hold stale outer-class references across hot-copied jars.

## Visual conventions

The migration also established the ACG-derived visual baseline:

- red/yellow/green stock category headings;
- dark-gray nested headings and neutral controls;
- green buy/increment controls;
- red sell/decrement controls;
- purple bulk/sell-side controls where currently used;
- white ordinary text, gray disabled text;
- disabled controls rendered as inert WP shells instead of disabled Starsector buttons.

## Evidence / Provenance

- Summarized from the long `Completed Meaningful Work` section removed from `PLANS.md` by commit `a0e647b` (`Debloat repo documentation`).
- Current architecture is in `HANDOVER.md`; detailed UI rules are in `.agent/archive/deep-dives/starsector-ui.md`.
