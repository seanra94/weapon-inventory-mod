# Weapons Procurement Packaging Notes

## Two Distributions

The mod currently has two user-facing paths:

- Clean popup path:
  - normal Starsector mod files only;
  - `F8` opens the Weapon Stock Review popup in valid market/trade contexts;
  - no core jar patching required;
  - intended as the forum-safe default distribution.

- Optional patched badge path:
  - same normal mod files plus a deterministic patch to `starfarer_obf.jar`;
  - adds ownership-count badges inside vanilla weapon and fighter LPC cargo cells;
  - should be treated as personal/advanced-use packaging unless the distribution target explicitly accepts core-jar patch instructions.

The clean popup must remain useful and buildable if the patched badge tooling is removed later.

## Clean Package

Include:

- `data/`
- `graphics/`
- `jars/weapons-procurement.jar`
- `mod_info.json`

Do not require users to run the patcher for the clean package. In clean builds, keep `wp_enable_patched_badges=false` by default.

## Patched Badge Package

Include the clean package plus clear instructions for:

1. Backing up or restoring the vanilla core jar.
2. Running `tools/cargo-stack-view-patcher.ps1 -Mode Restore`.
3. Running `tools/cargo-stack-view-patcher.ps1 -Mode Patch`.
4. Running `tools/validate-cargo-stack-view-patch.ps1`.
5. Restarting Starsector before testing.

Do not ship a prepatched `starfarer_obf.jar`. The patcher should operate on the user's installed game copy.

## Validation Before Release

For code or asset changes:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-total-badges.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-cargo-stack-view-patch.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

For clean-only documentation changes, run `tools\validate-doc-links.ps1` and `git diff --check`.

## Trade Rollback Fault Validation

`StockPurchaseExecutor` has an internal, disabled-by-default fault hook for validating rollback of WP-touched cargo counts and player credits. Temporarily set the JVM system property `wp.debug.failTradeStep` to one of these values during a local test run:

- `after-source-removal`
- `after-player-cargo-remove`
- `after-player-cargo-add`
- `after-target-cargo-add`
- `after-credit-mutation`

Use it only for manual validation, then clear the property before normal play or packaging. Test local buy, local sell, Sector Market buy, Fixer's Market buy, and mixed sell-then-buy plans, and confirm player cargo, touched market cargo, and credits return to their pre-confirm values after the forced failure.
