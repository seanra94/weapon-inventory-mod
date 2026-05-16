# Weapons Procurement Brief

Last updated: 2026-05-17

## Current State

Weapons Procurement is a Starsector `0.98a` mod for reviewing weapon and fighter LPC stock, planning buys/sells, and confirming trades from market/storage dialogs.

The primary product path is the clean `F8` popup. The patched cargo-cell badge path is optional, advanced-use, and isolated from the clean popup.

The source tree is fully migrated to Gradle/Kotlin. `build.ps1` remains the normal entry point and delegates to the Gradle wrapper; LazyLib is a required dependency because it supplies the Kotlin runtime in Starsector.

## Known-Good Source State

- Current branch: `main`
- Last pushed baseline before this brief update: `ab67cb4` (`Split trade packages by ownership`)
- Version in `mod_info.json`: `0.2.0`

## Commands

Docs-only:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

Runtime/source:

```powershell
$env:STARSECTOR_DIRECTORY = "X:\Path\To\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-total-badges.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-cargo-stack-view-patch.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

## Deploy Target

- Live mod folder: `C:\Games\Starsector\mods\Weapons Procurement`
- Deploy command: `tools/deploy-live-mod.ps1`
- The deploy script clean-syncs repo-managed clean-package files, or stages and queues a background deploy if the live jar is locked.
- Docs-only changes normally do not need deploy.

## Current Risks

- Runtime rollback fault validation still needs in-game evidence.
- Starsector classloading can keep stale jar/class state until restart.
- Luna settings, data/config files, graphics, and metadata matter; jar parity alone is not sufficient for data-heavy changes.
- The optional bytecode-patched badge path is high-risk and should remain advanced/private unless explicitly approved for a release target.
- Public release to `Shattersphere-Mods` must be curated. Do not mirror this private repo because it contains agent docs, local/private references, and optional patched-badge/bytecode material.

## Next Best Step

For code/runtime work, inspect `PLANS.md` and the relevant archive deep dive through `.agent/archive/INDEX.md`. For public release/export work, start with `.agent/PUBLIC_RELEASE.md`. For docs-only work, use the docs-only checks and avoid deployment.
