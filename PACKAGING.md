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

Do not require users to run the patcher for the clean package. The Luna setting `wp_enable_patched_badges` can stay present, but it has no visible effect without a patched core jar.

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
git diff --check
```

For clean-only documentation changes, `git diff --check` is usually enough.
