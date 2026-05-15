# Product and validation history

Status: historical
Scope: Weapons Procurement product boundary, validation posture, release-prep history, and avoid-list provenance
Last verified: 2026-05-12, summarized from the pre-debloat `PLANS.md`
Read when: investigating why the clean popup is the public path, why patched badges are isolated, or why certain approaches are on the avoid list
Do not read for: ordinary implementation details already covered by current `HANDOVER.md`
Related files: `README.md`, `PACKAGING.md`, `CHANGELOG.md`, `tools`, `.github/workflows/sanity.yml`
Search tags: `clean popup`, `patched badges`, `CargoStackView`, `late over-icon`, `release prep`, `Sanity`, `deploy-live-mod`

## Summary

- Exact in-cell cargo badges required bytecode patching; the clean popup became the public/default product to avoid making public release depend on core-jar patching.
- The patched badge path remains optional and isolated.
- The product evolved from badge diagnostics into a clean stock-review/trade-planning popup with weapons and fighter LPC support.
- Release readiness added packaging docs, config docs, clean deploy, live jar validation, doc validation, and GitHub sanity checks.
- Several visually tempting badge approaches are explicitly avoided because they regressed in runtime testing.
- Active runtime validation remains more important than static proof for UI, Luna, classloader, and transaction behavior.

## Index

- `Product boundary`: clean popup vs optional patched badges.
- `Badge path history`: why the current badge approach is constrained.
- `Release validation history`: packaging/deploy/CI/docs checks added over time.
- `Avoid-list provenance`: approaches that should not be retried casually.

## Details

## Product boundary

The old plan recorded the strategic conclusion:

- exact in-cell badges require patching vanilla cargo-cell rendering;
- a pure normal-mod version needs a different UI surface;
- therefore the clean `F8` stock-review popup is the real/public mod path;
- patched cargo-cell badges should stay personal/advanced unless the clean UI cannot solve a specific need.

This boundary led to:

- patched badges defaulting off;
- clean package instructions;
- optional patched package instructions;
- no prepatched `starfarer_obf.jar` distribution;
- popup code staying independent of the badge patcher.

## Badge path history

Completed badge-path work included:

- replacing raw GL/external hook attempts with direct sprite rendering in patched `CargoStackView`;
- moving count computation out of patched core into normal paused campaign code;
- adding player cargo plus accessible storage counts;
- adding fighter LPC support via cargo fighter counts;
- collapsing diagnostics to one precomposed total badge;
- removing old three-square diagnostics and stale marker assets/settings;
- adding `wp_enable_patched_badges` so patched helper returns `null` when disabled;
- adding explicit patch verification for target method, WEAPONS guard, helper embedding, helper call counts, sprite render calls, and stale patch patterns.

The current visual baseline came from runtime testing:

- bottom-right placement;
- stable pre-scale render frame;
- one precomposed badge sprite;
- no late over-icon rendering;
- no layered text/background rendering.

## Release validation history

Release-prep work completed over time:

- `PACKAGING.md` with clean-popup packaging and optional patched-badge patch/restore guidance;
- `CONFIG.md` with `perItem`, `W:` / `F:` item keys, legacy `perWeapon`, blacklists, multipliers, and debug hooks;
- `CHANGELOG.md` and version bump to `0.2.0`;
- `tools/deploy-live-mod.ps1` to copy data/config, graphics, jars, metadata, and docs together;
- `tools/validate-live-gui-classes.ps1` and `tools/validate-jar-classes.ps1`;
- `tools/validate-doc-links.ps1`;
- GitHub `Sanity` workflow with committed whitespace and repo-jar stale-class checks;
- clean/sync deploy behavior that removes stale repo-managed live files.

The old plan also recorded that `tools/validate-live-gui-classes.ps1` should be kept current as helper ownership changes.

## Avoid-list provenance

Do not retry these casually:

- late over-icon badge rendering: caused invisibility/blur regressions;
- layered background plus text badge rendering: unsuitable for tiny cargo-cell badges;
- campaign-state or LunaLib calls inside `WeaponsProcurementBadgeHelper`: patched-core helper context is not safe for campaign state;
- runtime reflection or raw GL rendering for the badge path;
- seller-detail rows or source-specific local-buy actions without a deliberate UI design pass;
- treating build/static validation as proof of runtime Starsector UI behavior.

## Evidence / Provenance

- Summarized from deleted completed/current-state sections of `PLANS.md` before commit `a0e647b` (`Debloat repo documentation`).
- Current product boundary is in `README.md`, `PACKAGING.md`, and `HANDOVER.md`.
