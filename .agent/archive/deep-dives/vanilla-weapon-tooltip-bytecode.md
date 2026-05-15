# Vanilla Weapon Tooltip Bytecode Deep Dive

Status: active-reference
Scope: Weapons Procurement, Starsector 0.98a campaign cargo/refit-style weapon hover tooltips
Last verified: 2026-05-16 against `C:\Games\Starsector\starsector-core\starfarer_obf.jar.wp_backup`
Read when: attempting to make stock-review weapon rows match vanilla hover tooltips, cargo-cell tooltip behavior, or codex weapon tooltip layout
Do not read for: ordinary stock list layout, trade execution, source pricing, or optional badge rendering unless a tooltip change crosses into private UI classes
Related files: `src/weaponsprocurement/gui/StockReviewItemTooltip.java`, `src/weaponsprocurement/gui/WimGuiControls.java`, `src/weaponsprocurement/gui/WimGuiListRowRenderer.java`
Search tags: vanilla tooltip, weapon hover, CargoDataGridView, CargoTooltipFactory, StandardTooltipV2, BaseWeaponSpec, bytecode, codex

## Summary

- Vanilla cargo-grid weapon hover tooltips are not built through the public `TooltipMakerAPI.TooltipCreator` path used by Weapons Procurement rows.
- The vanilla path is `CargoDataGridView.createItemView()` -> `assignTooltipToView()` -> `createTooltipForStack()` -> `CargoTooltipFactory` -> `StandardTooltipV2`.
- The exact vanilla weapon tooltip depends on private/obfuscated classes including `CargoStackView`, `CargoItemStack`, `CargoDataGridView$o`, `F`, `BaseWeaponSpec`, `StandardTooltipV2`, and `StandardTooltipV2Expandable`.
- Vanilla attaches the tooltip directly to `CargoStackView` with private UI methods, then adjusts placement in a `beforeShowing` runnable.
- The visible tooltip is a wrapper plus an embedded detailed element: title/design/description/price/owned-count first, then `Primary data`, custom primary text, `Ancillary data`, and custom ancillary text.
- Width is `400f`; the detailed inner element uses `StandardTooltipV2Expandable.createAsUIElement(400f)`, a `10f` vertical pad, a `3f` small pad, vanilla highlight color, and an `80f` weapon codex/icon block.
- The detailed data is heavily conditional. It branches on beam/projectile/missile spec subtype, ammo/recharge behavior, damage tags, compare mode, campaign state, and whether a real cargo stack/context exists.
- Copying the vanilla implementation verbatim into the clean/public mod path is not appropriate. It would require private Starsector classes or copied decompiled code, and would make public release compatibility brittle.
- The public-safe route is a clean-room approximation using public APIs and the bytecode behavior map below. The private-only route would be an explicitly approved experiment that must be excluded from public export.

## Index

- `Entry Path`: How vanilla cargo cells create and attach weapon tooltips.
- `Wrapper Tooltip`: The outer `StandardTooltipV2Expandable` behavior.
- `Detailed Weapon Element`: The inner vanilla weapon data block.
- `Conditional Data Rules`: Tags and context that change rows.
- `Why Verbatim Copy Is Unsafe`: Public release and runtime risks.
- `Implementation Options`: Practical paths for this mod.
- `Evidence / Provenance`: Bytecode commands and files used.

## Details

### Entry Path

Vanilla cargo-grid cells are created by `CargoDataGridView.createItemView(Object)`. The method:

- casts the item to `CargoItemStack`;
- creates a `CargoStackView(stack, scroller)`;
- applies red-background state using the grid manifest's `preventPickup(stack)`;
- calls `assignTooltipToView(view, stack, manifest, stats, transferHandler)`;
- positions the `CargoStackView` in a `100f` cell grid.

`createTooltipForStack(stack, manifest, stats, transferHandler)` dispatches by stack type. For weapons:

- reads `stack.getWeaponIfWeapon()` as a private `BaseWeaponSpec`;
- calls the weapon overload in `CargoTooltipFactory` with the spec, character stats, cargo stack, manifest, and transfer handler;
- returns a private `StandardTooltipV2`.

`assignTooltipToView(...)` then calls `CargoStackView.setTooltip(0f, tooltip)` and installs a `beforeShowing` runnable. That runnable sets tooltip placement with:

- x offset: negative half of tooltip width;
- y offset: negative tooltip height plus `20f`.

This is fundamentally different from WP's current `TooltipMakerAPI.addTooltipTo(TooltipCreator, UIComponentAPI, BELOW)` path.

The relevant private method chain and descriptors are:

```text
CargoDataGridView.createItemView(Object)
  -> createTooltipForStack(CargoItemStack, CargoDataGridView$o, CharacterStats, F)
  -> CargoTooltipFactory.super(BaseWeaponSpec, CharacterStats, CargoItemStack, CargoDataGridView$o, F)
       descriptor:
       (Lcom/fs/starfarer/loading/specs/BaseWeaponSpec;
        Lcom/fs/starfarer/campaign/CharacterStats;
        Lcom/fs/starfarer/campaign/ui/trade/CargoItemStack;
        Lcom/fs/starfarer/campaign/ui/trade/CargoDataGridView$o;
        Lcom/fs/starfarer/campaign/ui/trade/F;)
       Lcom/fs/starfarer/ui/impl/StandardTooltipV2;
  -> new CargoTooltipFactory$4(...)
  -> CargoStackView.setTooltip(0f, StandardTooltipV2)
  -> StandardTooltipV2.setBeforeShowing(CargoDataGridView$2)
```

`StandardTooltipV2` also has public-looking static bytecode methods named `createWeaponTooltip(...)`, but their signatures still use private `BaseWeaponSpec`, `CargoItemStack`, `CargoDataGridView$o`, `F`, and `StandardTooltipV2`. They do not provide a public `TooltipMakerAPI.TooltipCreator` bridge.

### Wrapper Tooltip

The cargo weapon factory returns a private anonymous/inner tooltip class, `CargoTooltipFactory$4`, extending `StandardTooltipV2Expandable`. Its `createImpl(boolean expanded)` does the top-level cargo hover work:

- if the weapon has `codex_unlockable`, calls `SharedUnlockData.reportPlayerAwareOfWeapon(id, true)`;
- calls `setCodexEntryId(CodexDataV2.getWeaponEntryId(id))`;
- adds the weapon name as title using `BaseWeaponSpec.getWeaponName()`;
- adds manufacturer/design text through `Misc.addDesignTypePara(...)`;
- loads `Description.Type.WEAPON` using the weapon id;
- adds the description's first paragraph;
- if the description's second text begins with `-`, italicizes the first paragraph and adds the second text in gray;
- when a real cargo stack is present, adds a quantity label and a cost label;
- in campaign state, counts player-owned weapons across player cargo and storage access markets, then adds an owned-count paragraph;
- embeds the detailed weapon element produced by `CargoTooltipFactory.super(BaseWeaponSpec, CharacterStats, ship weapon slot, callback)`.

The wrapper means vanilla cargo hovers are not merely "weapon stats"; they also include cargo context and campaign ownership context when available.

### Detailed Weapon Element

The detailed element is created with `StandardTooltipV2Expandable.createAsUIElement(400f)`. The bytecode sets:

- main width: `400f`;
- normal section pad: `10f`;
- small pad: `3f`;
- highlight color: `Misc.getHighlightColor()`;
- icon/custom block size: `80f`.

The method collects data from private weapon spec subtypes and projectile specs, then lays out the visible data as follows.

`Primary data` section:

- vanilla codex weapon visual block;
- one-column grid alongside the visual block;
- `Primary role`;
- `Mount type`;
- conditional mount notes for hybrid, synergy, composite, universal, and "counts as" stat modifiers;
- `Ordnance points`;
- `Range`;
- damage row, usually labeled `Damage`, but changed to `Special` for `damage_special`;
- optional `EMP damage`;
- DPS rows such as `Damage / second`, `Damage / second (sustained)`, and related combined current/sustained formatting;
- flux rows such as `Flux / second`, `Flux / second (sustained)`, `Flux / shot`, `Flux / damage`, and `Flux / non-EMP damage`;
- conditional limited ammo/charges note, including "No flux cost to fire" variants.

After the primary grid, vanilla adds `BaseWeaponSpec.getCustomPrimary()` if present. Highlight strings are split on `|`, trimmed, and rendered using the vanilla highlight color.

`Ancillary data` section:

- damage type icon and `Damage type`;
- damage type explanatory text;
- `Speed`;
- `Tracking`;
- `Hitpoints`;
- `Accuracy`;
- `Turn rate`;
- ammo/charge/reload/recharge rows, depending on ammo behavior and weapon type;
- `Firing cycle` or `Refire delay`;
- `Burst size`;
- beam cycle/timing details for burst beam cases.

After the ancillary grid, vanilla adds `BaseWeaponSpec.getCustomAncillary()` if present, again splitting highlight strings on `|`.

### Conditional Data Rules

The bytecode shows these important branches:

- `no_standard_data` suppresses much of the normal stats block.
- `damage_special` changes the main damage value to `Special`.
- `damage_soft_flux` adds soft-flux/no-hard-flux wording to damage/flux description.
- `isNoDPSInTooltip()` suppresses DPS rows.
- `usesAmmo()`, `getAmmoPerSecond()`, `getMaxAmmo()`, and `getReloadSize()` determine whether rows say ammo, charges, reload, recharge, seconds per reload, or gained charges.
- Energy weapons with regenerating charges use charge wording; non-energy ammo weapons use ammo/reload wording.
- Projectile, missile, MIRV, beam, and burst-beam internals each have different calculations.
- Compare mode can use a `CargoTooltipFactory$o` callback and `MutableStat` rows to display deltas against another weapon.
- Campaign-state cargo hovers count weapons in player cargo plus accessible storage markets.

### Why Verbatim Copy Is Unsafe

The exact vanilla tooltip cannot be copied into the clean/public WP row path without crossing private boundaries:

- WP's current build classpath uses `starfarer.api.jar` plus ordinary dependency jars, not `starfarer_obf.jar`.
- The factory takes `BaseWeaponSpec`, not just public `WeaponSpecAPI`.
- The cargo hover entry point takes private `CargoItemStack`, `CargoDataGridView$o`, and `F` context objects.
- The rendered tooltip object is private `StandardTooltipV2`, not a public `TooltipMakerAPI.TooltipCreator`.
- The attach point is private `CargoStackView.setTooltip(...)`, not public `TooltipMakerAPI.addTooltipTo(...)`.
- The detailed block uses private projectile/spec classes and vanilla codex UI implementation classes.
- A literal copy of decompiled code would be brittle, version-sensitive, and unsuitable for public release.

Reflection or direct imports could make a private-only prototype possible, but this should be treated like badge bytecode work: explicit approval only, private build only, excluded from public export, and validated in-game.

### Implementation Options

Recommended clean/public option:

- Keep using `TooltipMakerAPI.TooltipCreator`.
- Rebuild a public-API approximation that follows the vanilla order and wording where public APIs expose the same data.
- Add title, design/manufacturer, description first paragraph, owned count, primary data, custom primary, ancillary data, and custom ancillary.
- Do not try to instantiate `StandardTooltipV2`, private codex icon widgets, private cargo stack views, or private projectile classes in the public build.
- Treat the bytecode map as a layout/wording reference, not as source code to copy.

Private experimental option:

- Compile or reflect against `starfarer_obf.jar` and call `CargoTooltipFactory`/`StandardTooltipV2` directly.
- Attach only if a stable private UI component handle can be proven.
- Keep this out of public export and behind explicit approval, because it creates the same kind of release-hostile private-core dependency as the badge patching path.

Not recommended:

- Copy decompiled vanilla source into WP.
- Patch `CargoDataGridView` or `CargoStackView` for tooltip access.
- Depend on the currently patched live core jar as evidence for vanilla behavior; use the unpatched backup for bytecode reads.

## Evidence / Provenance

Bytecode inspection used an unpatched backup because the live `starfarer_obf.jar` currently contains patched badge calls:

```powershell
$cp = "$env:TEMP\wp-vanilla-tooltip-bytecode\starfarer_obf_unpatched_backup.jar;C:\Games\Starsector\starsector-core\starfarer.api.jar"
javap -classpath $cp -c -p -s com.fs.starfarer.campaign.ui.trade.CargoDataGridView
javap -classpath $cp -c -p -s com.fs.starfarer.campaign.ui.trade.CargoStackView
javap -classpath $cp -c -p -s com.fs.starfarer.ui.impl.CargoTooltipFactory
javap -classpath $cp -c -p -s com.fs.starfarer.ui.impl.CargoTooltipFactory`$4
javap -classpath $cp -c -p -s com.fs.starfarer.ui.impl.StandardTooltipV2
javap -classpath $cp -c -p -s com.fs.starfarer.ui.impl.StandardTooltipV2Expandable
```

Key bytecode anchors:

- `CargoDataGridView.createItemView(...)`: creates `CargoStackView`, then calls `assignTooltipToView(...)`.
- `CargoDataGridView.createTooltipForStack(...)`: weapon branch calls `CargoTooltipFactory.super(BaseWeaponSpec, CharacterStats, CargoItemStack, CargoDataGridView$o, F)`.
- `CargoDataGridView.assignTooltipToView(...)`: calls `CargoStackView.setTooltip(0f, StandardTooltipV2)` and `StandardTooltipV2.setBeforeShowing(...)`.
- `CargoDataGridView$2.run()`: computes tooltip offset from `StandardTooltipV2.getPosition()`.
- `CargoTooltipFactory$4.createImpl(...)`: wrapper title, design text, description, cargo quantity/cost, campaign-owned count, and embedded detailed weapon element.
- `CargoTooltipFactory.super(BaseWeaponSpec, CharacterStats, ship slot, callback)`: detailed primary and ancillary weapon data element.
