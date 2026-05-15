# Starsector UI deep dive

Status: active-reference
Scope: Weapons Procurement stock-review campaign UI and shared `WimGui*` helpers
Last verified: 2026-05-12, archived from the pre-debloat `HANDOVER.md`
Read when: changing stock-review layout, row sizing, indentation, `WimGui*`, buttons, scrolling, text fitting, helper extraction, or live-jar class validation
Do not read for: pure config copy edits, release-note updates, or non-UI trade math
Related files: `src/weaponsprocurement/gui`, `src/weaponsprocurement/gui/wimgui`, `tools/validate-live-gui-classes.ps1`
Search tags: `WimGui`, `buttonPressed`, `indent`, `a02e507`, `NoClassDefFoundError`, `validate-live-gui-classes`

## Summary

- Keep the popup on the explicit custom-panel/list architecture.
- Preserve the equal-indent, equal-width-reduction layout model; commit `a02e507` is the known-good right-edge alignment reference.
- Keep row/button actions explicit and routed through shared `WimGui*` controls.
- Preserve the event-gated button polling fallback because nested Starsector buttons are not fully reliable through `buttonPressed(...)`.
- Keep shared scroll, text, modal, and row-cell math centralized.
- Treat classloader-sensitive GUI helper changes as runtime risks even when compile/jar checks pass.
- Validate live jar contents after helper extraction and restart Starsector before testing new helper classes.
- Do not reload this full file for non-UI tasks.

## Index

- `Screen Structure`: controller, renderer, and list-model ownership.
- `Row Hierarchy And Indentation`: nested row alignment rules.
- `Fixed Cells And Data Rows`: storage/price/plan/data-row sizing conventions.
- `Category Headings And Labels`: heading and label conventions.
- `Button And Input Rules`: button rendering and fallback polling constraints.
- `Colors And Visual Conventions`: accepted ACG-derived palette.
- `Scroll, Text, And Modal Layout`: shared list/modal/text behavior.
- `Runtime Classloader Lessons`: classloader and live-jar validation cautions.

## Details

## Screen Structure

- The clean popup renders through an explicit custom-panel shell, not one long `TooltipMakerAPI` row pile.
- `StockReviewRenderer` owns the shell, header, action row, footer, mode-specific row selection, and stock-specific scroll rows/top gaps.
- `StockReviewListModel` builds Make Trades section/category rows.
- `StockReviewReviewListModel` builds Review Trades rows.
- `StockReviewItemInfoRows` owns shared Basic/Advanced item detail rows.
- `StockReviewTradeRowCells` owns shared storage/price/plan/action cells.
- `StockReviewFooterRenderer` owns mode-specific footers.
- `StockReviewModeController` owns review/filter/color-debug mode state.
- `StockReviewUiController` owns expansion, source/sort toggles, filters, color debug, scroll indicators, review entry/back, reset-all, and close/Escape behavior.
- `StockReviewTradeController` owns row buy/sell adjustments, reset, purchase-all-until-sufficient, and sell-all-until-sufficient.
- `StockReviewExecutionController` owns confirm-trade checks, execution ordering, per-line failure handling, cleanup, review exit, and post-confirm refresh/reopen routing.

## Row Hierarchy And Indentation

Known-good reference: commit `a02e507` was user-confirmed as the point where stock-review indentation and right-edge sizing worked.

The intended hierarchy is:

```text
Weapons/Wings
  No/Insufficient/Sufficient Stock
    Weapon or wing row
      Basic/Advanced Info
        Data row
```

Rules:

- Each level adds exactly one indent step.
- A child row starts one indent deeper and reduces visible width by the same indent amount.
- Right edges must align across parent/child/grandchild rows.
- Implement this with an invisible, borderless indent spacer plus an equal width reduction, not by shifting a full-width row right.
- Indented spacer regions must stay black/empty and must not draw colored fills or borders.
- Weapon rows, review rows, and button hitboxes use white grid borders.
- Nested toggle headings use the ACG dark-gray collapsible heading fill.
- The top stock category headings use red/yellow/green fills for No/Insufficient/Sufficient stock.

## Fixed Cells And Data Rows

- Item rows use this order: item label, `Storage`, `Price`, `Buying`/`Selling`, dynamic sell step, `-1`, `+1`, dynamic buy step, `Sufficient`, `Reset`.
- Width increases for `Storage`, `Price`, or `Buying`/`Selling` should usually come out of the item label/toggle-heading width.
- `Storage` is intentionally wider than the other compact cells and is left-aligned with normal WP padding.
- Data rows use a 65% label / 35% value split so labels such as `EMP/Second` and `Turn Rate/Second` fit.
- `Storage` shows full owned stock under the active owned-source policy, including player inventory and accessible storage.
- When a plan exists, append a signed delta, for example `Storage: 6 [-2]` or `Storage: 6 [+2]`.
- Compact row caps from the old width stress test:
  - storage/plan counts cap visually at `99+`;
  - unit prices cap visually at `99,999c`;
  - plan totals cap visually at `999,999c`.

## Category Headings And Labels

- Top-level stock headings summarize visible category counts and queued units.
- Under Weapons, use weapon type counts.
- Under Wings, use wing type counts.
- `Selling` and `Buying` queued totals must be counted separately, not netted.
- `Price` is the user-facing and code-facing name for unit item price.
- `StockSortMode.fromConfig(...)` may accept legacy `COST`, but new config/code should use `PRICE`.
- Longer campaign messages may say `credits`, but compact row credit values should use the Starsector-supported cent glyph where the runtime font supports it.

## Button And Input Rules

- Use real Starsector buttons with blank built-in labels plus WP-rendered labels layered separately.
- Keep `WimGuiButtonBinding` / `WimGuiButtonPoller` as the event-gated fallback because nested custom-panel controls did not reliably arrive through `buttonPressed(...)` alone.
- The fallback should poll only for a few frames after mouse down/up events.
- Keep row/button actions as explicit `StockReviewAction` ids. Do not infer state from checkbox state.
- Disabled controls should render as inert WP-owned shells with gray text and disabled fill, not disabled Starsector buttons.
- Avoid bare `addAreaCheckbox(...)` visuals for action rows.
- Avoid `addParaWithMarkup()` and highlighted `addPara(...)` overloads for row labels and item text. `%` and markup can produce formatting/clipping problems.

## Colors And Visual Conventions

The accepted ACG-derived palette:

- No Stock category rows: cancel red.
- Insufficient rows: load yellow.
- Sufficient rows: confirm green.
- Buy/increment buttons: confirm green.
- Sell/decrement buttons: cancel red.
- Bulk trade buttons: purple.
- Nested toggle headings and general top controls: dark gray.
- Neutral available rows: black/dark action background.
- Disabled/locked rows: disabled shell plus gray text.

Other rules:

- Ordinary popup text should stay white/default-font.
- Starsector dims idle button interiors heavily. Hover/glow can appear closer to raw RGB than idle state.
- WP-owned row fills sit behind Starsector buttons while button backgrounds are dimmed, recreating the ACG inner dimmed rectangle with brighter outer row fill.
- `Credits Available` and `Cargo Space Available` deltas use color to show direction:
  - losing credits/space should read as bad;
  - gaining credits/space should read as good.

## Scroll, Text, And Modal Layout

- `WimGuiScroll`, `WimGuiScrollSlice`, `WimGuiScrollableListState`, `WimGuiModalListLayout`, and `WimGuiModalListRenderer` own shared scroll math and row slicing.
- Preserve scroll offsets and expanded headings across content rebuilds.
- Do not reserve scroll indicators when all rows fit.
- Page/indicator movement should use the actual visible row capacity.
- ASCII indicators are safer than arrow glyphs in this UI.
- `WimGuiText` / `WimGuiTextLayout` own word-aware fitting/wrapping, long-token handling, weak-line-ending rebalancing, and growable row-height calculation.
- Long copy should use shared wrapping helpers rather than local per-screen fixes.
- Future modal popups should use the ACG three-section template: heading, body, bottom buttons.
- Do not rely on click-out-to-close for modals; Escape and explicit Close/Cancel are sufficient.
- If outside-click behavior is ever added, use raw-coordinate inside/outside checks rather than a full-screen backdrop bound directly to Cancel.

## Runtime Classloader Lessons

- Prefer stable explicit classes over anonymous/local/lambda-generated classes in runtime-sensitive UI paths.
- After adding GUI helper classes, verify the live jar contains them and restart Starsector before testing.
- Starsector can keep an older jar index in a running process; hot-copying a jar while the game is open can still produce `NoClassDefFoundError`.
- After renaming/removing nested classes, consider a short-lived compatibility shim if an old outer class may already be loaded.
- Keep `tools/validate-live-gui-classes.ps1` current when helper ownership changes.

## Evidence / Provenance

- Extracted and cleaned from the detailed `HANDOVER.md` that existed before commit `a0e647b` (`Debloat repo documentation`).
- Commit `a02e507` is preserved here as the user-confirmed layout reference for nested row alignment.
- Keep this file as a reference, not an active task list.
