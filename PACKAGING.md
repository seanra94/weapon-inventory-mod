# Weapons Procurement Packaging Notes

## Clean Package

The public package is the clean procurement GUI:

- normal Starsector mod files only;
- `F8` opens the Weapon Stock Review popup in valid market/storage contexts;
- optional LunaLib market-dialog entry can add `Open "Weapon Procurement"`;
- no private docs, local paths, deploy queues, or internal archives.
- LazyLib and LunaLib remain required dependencies.

Include:

- `data/`
- `jars/weapons-procurement.jar`
- `mod_info.json`
- `README.md`
- `CONFIG.md`
- `CHANGELOG.md`
- `PACKAGING.md`

Include `graphics/` only if a future public GUI asset actually needs it.

Before public release, bump `mod_info.json` when appropriate and update `CHANGELOG.md`.

## Validation Before Release

For code or asset changes:

```powershell
$env:STARSECTOR_DIRECTORY = "X:\Path\To\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

`tools\deploy-live-mod.ps1` performs a clean sync of repo-managed clean-package files by default. If Starsector is locking the live jar, it stages the built files and queues a background deploy for after the lock clears. Use `-NoClean` only for local debugging when intentionally preserving extra live files.

For documentation-only changes:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

## Private Patched Badges

The patched cargo-cell badges are a private runtime path. A clean build does not contain the count bridge, so using a patched `starfarer_obf.jar` with a clean live mod jar can leave every badge on the red error sprite.

Use the private deploy wrapper for this path:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-private-badges.ps1 -StarsectorDir "X:\Path\To\Starsector"
```

The wrapper builds `build.ps1 -PrivateBadge`, verifies that the jar contains the private helper/config/updater classes, refreshes the embedded core helper through the cargo stack patcher, validates the patch, and deploys with `-AllowPrivateBadgeJar`.

If Starsector is running, the deploy step may queue. In that case, close Starsector and let the queued deploy finish before judging the badge behavior in game.

## Trade Rollback Fault Validation

`StockPurchaseExecutor` has an internal, disabled-by-default fault hook for validating rollback of WP-touched cargo counts and player credits. Temporarily start Starsector with the JVM system property `wp.debug.failTradeStep` set to one of these values during a local test run:

- `after-source-removal`
- `after-player-cargo-remove`
- `after-player-cargo-add`
- `after-target-cargo-add`
- `after-credit-mutation`

Use it only for manual validation, then remove the property or set it to `none` before normal play or packaging. Test local buy, local sell, Sector Market buy, Fixer's Market buy, and mixed sell-then-buy plans, and confirm player cargo, touched market cargo, and credits return to their pre-confirm values after the forced failure.
