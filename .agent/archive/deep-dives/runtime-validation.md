# Runtime validation deep dive

Status: active-reference
Scope: Weapons Procurement runtime, release, deploy, rollback, and patched-badge validation procedures
Last verified: 2026-05-12, archived from the pre-debloat `HANDOVER.md`
Read when: preparing manual in-game validation, rollback fault checks, patched badge checks, release validation, or deploy troubleshooting
Do not read for: small docs-only edits that do not affect validation commands
Related files: `PACKAGING.md`, `PLANS.md`, `tools/deploy-live-mod.ps1`, `tools/validate-live-gui-classes.ps1`, `tools/validate-cargo-stack-view-patch.ps1`, `tools/validate-total-badges.ps1`
Search tags: `deploy-live-mod`, `rollback`, `wp.debug.failTradeStep`, `validate-cargo-stack-view-patch`, `validate-total-badges`

## Summary

- Static/build validation is not in-game proof.
- Use docs-only checks for docs-only edits; use the full sequence for runtime/package changes.
- Validate the clean popup in real market/storage contexts.
- Validate each trade source and trade mode separately.
- Forced rollback testing remains the key outstanding runtime proof.
- Patched badge validation is separate from clean popup validation.
- Restart Starsector after class/helper/jar changes because stale classloader state can survive hot-copying.
- Live deploy stages files and queues a hidden worker when Starsector locks the jar; treat queued deploys as pending until live parity passes.
- Reset dangerous debug/fault settings to `none` before normal play or packaging.

## Index

- `Test-Ready Command Sequence`: command sequence for full local validation.
- `Clean Popup Manual Validation`: in-game popup checklist.
- `Trade Mode Manual Validation`: trade path matrix.
- `Rollback Fault Validation`: forced failure testing.
- `Patched Badge Manual Validation`: optional patched path checklist.
- `Runtime Caveats`: known runtime limitations.

## Details

## Test-Ready Command Sequence

Full local validation sequence for code, assets, patcher checks, deploy, and live jar checks:

```powershell
$env:STARSECTOR_DIRECTORY = "X:\Path\To\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\generate-total-badges.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\cargo-stack-view-patcher.ps1 -Mode Restore
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\cargo-stack-view-patcher.ps1 -Mode Patch
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-cargo-stack-view-patch.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-total-badges.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

If `deploy-live-mod.ps1` reports a queued deploy, close Starsector and wait for the background worker to publish the staged files. Then rerun `validate-live-gui-classes.ps1`.

For docs-only edits, normally use:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

## Clean Popup Manual Validation

1. Launch Starsector and load a save.
2. Open a market trade screen.
3. Press `F8` to open Weapon Stock Review.
4. Confirm the popup groups stock items under No Stock, Insufficient Stock, and Sufficient Stock.
5. Confirm top-level `Weapons` and `Wings` headings both work when relevant stock exists.
6. Confirm item entries show stable `Storage`, unit `Price`, and planned `Buying`/`Selling` cells while queued changes are adjusted.
7. Confirm `Source` and `Black Market` buttons rebuild the snapshot without stale layered text and without closing/reopening the popup.
8. Confirm `Sort` cycles through `Stock`, `Name`, and `Price` without collapsing headings unexpectedly.
9. Confirm item rows expand into Basic Info / Advanced Info rows.
10. Confirm mouse-wheel scrolling and clickable ASCII scroll indicators preserve state and do not appear when all rows fit.
11. Confirm `+1`, `-1`, dynamic step buttons, `Sufficient`, and reset controls work from row-level entries.
12. Confirm credits/space failures are blocked with warnings rather than partial mutation.
13. Confirm no GUI crash.

## Trade Mode Manual Validation

Validate these separately where practical:

- local legal buy;
- local black-market buy;
- local legal sell;
- local black-market sell;
- Sector Market buy;
- Fixer's Market buy;
- mixed sell-then-buy plan;
- insufficient credits;
- insufficient cargo space;
- source stock drained between review and confirm.

Expected behavior:

- `Confirm Trades` is the only action that mutates cargo/credits.
- Planned sells can fund planned buys.
- Stale stock should fail before mutation.
- Transaction callbacks should occur only after cargo/credit mutation succeeds.
- Fixer's Market buys are virtual and do not drain real cargo.
- Sector Market buys drain real remote cargo.
- Remote-mode sells use the current local legal buyer.

## Rollback Fault Validation

The remaining high-value runtime validation is forced rollback testing.

1. Start Starsector with JVM property `wp.debug.failTradeStep` set to each supported failure step.
2. Test at least:
   - local buy;
   - local sell;
   - Sector Market buy;
   - Fixer's Market buy;
   - mixed sell-then-buy plans.
3. Confirm WP-touched cargo counts and player credits return to pre-confirm values.
4. Confirm transaction callback side effects do not fire before rollbackable trade commit.
5. Reset the setting to `none` before normal play or packaging.

Do not treat build success as proof of rollback behavior. This needs in-game evidence.

## Patched Badge Manual Validation

Only for the optional patched path:

1. Restore then patch `starfarer_obf.jar` using the repo scripts.
2. Run `validate-cargo-stack-view-patch.ps1`.
3. Launch/load a save.
4. Open a market trade screen.
5. Confirm commodities remain vanilla.
6. Confirm weapon stacks show one bottom-right badge.
7. Confirm fighter LPC stacks show one bottom-right badge.
8. Confirm totals include player cargo plus all accessible storage.
9. Confirm buy/sell/storage moves update while paused.
10. Confirm disabling patched badges in Luna makes the embedded helper skip rendering.

## Runtime Caveats

- Compile success and jar validation do not prove campaign/refit/combat UI paths.
- Starsector classloaders may keep stale jar/class state while the game is running; restart after helper/class changes.
- LunaLib setting changes should be tested from the actual game UI when they affect runtime behavior.
- Runtime testing currently suggests black-market penalties fire through `reportPlayerMarketTransaction`, but keep this as observed behavior, not a license to simulate extra reputation/suspicion effects without evidence.

## Evidence / Provenance

- Extracted and cleaned from the detailed `HANDOVER.md` that existed before commit `a0e647b` (`Debloat repo documentation`).
- The rollback-fault item remains runtime validation work, not a statically proven result.
- Use `PACKAGING.md` for release commands if it diverges from this historical reference.
