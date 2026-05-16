package weaponsprocurement.gui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.codex.CodexDataV2
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.core.CreditFormat
import weaponsprocurement.core.SubmarketWeaponStock
import weaponsprocurement.core.WeaponStockRecord
import java.awt.Color
import java.util.Locale

class StockReviewItemTooltip private constructor(
    private val record: WeaponStockRecord,
    private val toggleText: String?,
) : TooltipMakerAPI.TooltipCreator {
    override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false

    override fun getTooltipWidth(tooltipParam: Any?): Float = if (record.isWing()) WING_TOOLTIP_WIDTH else WIDTH

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
        if (record.isWing()) {
            tooltip.setParaFontDefault()
            tooltip.setParaFontColor(textColor())
            tooltip.addTitle(record.displayName, titleColor())
            createWingTooltip(tooltip)
            if (WimGuiTooltip.hasText(toggleText)) {
                tooltip.addPara(
                    tooltipFormat(toggleText),
                    SECTION_PAD,
                    StockReviewStyle.MUTED,
                    highlightColor(),
                    "Basic Info",
                    "Advanced Info",
                )
            }
        } else {
            setCodexEntry(tooltip, CodexDataV2.getWeaponEntryId(record.itemId))
            addPaddedWeaponTooltip(tooltip)
        }
    }

    private fun addPaddedWeaponTooltip(tooltip: TooltipMakerAPI) {
        val panel = Global.getSettings().createCustom(WIDTH, TOOLTIP_LAYOUT_HEIGHT, BaseCustomUIPanelPlugin())
        val content = panel.createUIElement(CONTENT_WIDTH, TOOLTIP_LAYOUT_HEIGHT, false)
        content.setParaFontDefault()
        content.setParaFontColor(textColor())
        createWeaponTooltip(content)

        val contentHeight = maxOf(1f, content.heightSoFar)
        content.position.setSize(CONTENT_WIDTH, contentHeight)
        panel.addUIElement(content).inTL(OUTER_PAD_X, OUTER_PAD_TOP)
        panel.position.setSize(WIDTH, contentHeight + OUTER_PAD_TOP + OUTER_PAD_BOTTOM)
        tooltip.addCustom(panel, 0f)
    }

    private fun createWeaponTooltip(tooltip: TooltipMakerAPI) {
        val spec = record.spec ?: return
        tooltip.addTitle(record.displayName, titleColor())
        Misc.addDesignTypePara(tooltip, spec.manufacturer, SMALL_PAD)
        addDescription(tooltip)
        addCargoContext(tooltip)

        addSectionHeading(tooltip, "Primary data", SECTION_PAD)
        addIconGrid(
            tooltip,
            StockReviewWeaponIconPlugin.spriteName(spec),
            primaryRows(spec),
            true,
            StockReviewWeaponIconPlugin.motifType(spec),
            SECTION_CONTENT_PAD,
        )
        addSpecPara(tooltip, spec.customPrimary, spec.customPrimaryHL, CUSTOM_TEXT_PAD, spec)

        addSectionHeading(tooltip, "Ancillary data", SECTION_PAD)
        addIconGrid(tooltip, damageIconSpriteName(spec.damageType), ancillaryRows(spec), false, null, SECTION_CONTENT_PAD)
        addSpecPara(tooltip, spec.customAncillary, spec.customAncillaryHL, CUSTOM_TEXT_PAD, spec)
    }

    private fun createWingTooltip(tooltip: TooltipMakerAPI) {
        val spec: FighterWingSpecAPI = record.wingSpec ?: return
        setCodexEntry(tooltip, CodexDataV2.getFighterEntryId(record.itemId))
        tooltip.addSectionHeading(
            "Fighter LPC",
            StockReviewStyle.HEADING_BACKGROUND,
            StockReviewStyle.ROW_BORDER,
            Alignment.MID,
            SECTION_PAD,
        )
        beginStyledGrid(tooltip)
        tooltip.addToGrid(0, 0, "Role", format(spec.role))
        tooltip.addToGrid(1, 0, "OP", record.wingOpCostLabel)
        tooltip.addToGrid(2, 0, "Fighters", record.wingFighterCountLabel)
        tooltip.addToGrid(0, 1, "Range", record.rangeLabel)
        tooltip.addToGrid(1, 1, "Refit", record.wingRefitTimeLabel)
        tooltip.addToGrid(2, 1, "Desired", record.desiredCount.toString())
        tooltip.addGrid(SMALL_PAD)
    }

    private fun addDescription(tooltip: TooltipMakerAPI) {
        val description: Description = try {
            Global.getSettings().getDescription(record.itemId, Description.Type.WEAPON)
        } catch (_: RuntimeException) {
            null
        } ?: return
        val firstPara = description.text1FirstPara
        if (hasText(firstPara)) {
            val label = tooltip.addPara(tooltipFormat(truncateDescription(firstPara.trim())), SECTION_PAD)
            if (hasText(description.text2) && description.text2.trim().startsWith("-")) {
                label.italicize()
            }
        }
        if (hasText(description.text2) && description.text2.trim().startsWith("-")) {
            val label = tooltip.addPara(tooltipFormat(description.text2.trim()), SMALL_PAD, mutedColor())
            label.italicize()
        }
    }

    private fun addCargoContext(tooltip: TooltipMakerAPI) {
        val cargoSpace = cargoSpaceLabel()
        if (hasText(cargoSpace)) {
            addHighlightedPara(tooltip, "Cargo space: $cargoSpace per unit.", cargoSpace, SECTION_PAD)
        }

        val price = priceLabel()
        if (hasText(price)) {
            addHighlightedPara(tooltip, "Price: $price per unit.", price, SECTION_PAD)
        }

        val count = record.ownedCount.toString()
        val plural = if (record.ownedCount == 1) "weapon" else "weapons"
        addHighlightedPara(tooltip, "You own a total of $count $plural of this type.", count, SECTION_PAD)
    }

    private fun primaryRows(spec: WeaponSpecAPI): List<StatRow> {
        val rows = ArrayList<StatRow>()
        addRow(rows, "Primary role", format(spec.primaryRoleStr))
        addRow(rows, "Mount type", format(spec.size) + ", " + format(spec.mountType))
        addMountNotes(rows, spec)
        addRow(rows, "Ordnance points", record.opCostLabel)
        addRow(rows, "Range", record.rangeLabel)
        addRow(rows, damageLabel(spec), damageValue(spec))
        if (hasMeaningful(record.empLabel) && record.empLabel != "0") {
            addRow(rows, "EMP damage", record.empLabel)
        }
        if (!spec.isNoDPSInTooltip) {
            addRow(rows, "Damage / second", record.sustainedDamagePerSecondLabel)
        }
        addRow(rows, "Flux / second", record.sustainedFluxPerSecondLabel)
        addRow(rows, "Flux / shot", fluxPerShotLabel(spec))
        addRow(rows, "Flux / damage", record.fluxPerDamageLabel)
        return rows
    }

    private fun ancillaryRows(spec: WeaponSpecAPI): List<StatRow> {
        val rows = ArrayList<StatRow>()
        val damageType = spec.damageType
        addRow(rows, "Damage type", damageType?.displayName ?: "?")
        addRow(rows, "", damageMultiplierLabel(damageType))
        addRow(rows, "Speed", format(spec.speedStr))
        addRow(rows, "Tracking", format(spec.trackingStr))
        addRow(rows, "Accuracy", format(spec.accuracyStr))
        addRow(rows, "Turn rate", format(spec.turnRateStr))
        if (spec.burstSize > 1) {
            addRow(rows, "Burst size", spec.burstSize.toString())
        }
        addRow(rows, "Refire delay (seconds)", record.refireSecondsLabel)
        if (spec.usesAmmo()) {
            addRow(rows, "Ammo", record.maxAmmoLabel)
            addRow(rows, "Recharge / second", record.ammoGainLabel)
            addRow(rows, "Reload time (seconds)", record.secPerReloadLabel)
        }
        if (spec.isBeam) {
            addRow(rows, "Charge up", record.beamChargeUpLabel)
            addRow(rows, "Charge down", record.beamChargeDownLabel)
        }
        return rows
    }

    private fun addIconGrid(
        tooltip: TooltipMakerAPI,
        spriteName: String?,
        rows: List<StatRow>,
        weaponTile: Boolean,
        motifType: WeaponAPI.WeaponType?,
        pad: Float,
    ) {
        if (rows.isEmpty()) {
            return
        }
        val visibleRows = maxOf(1, rows.size)
        val height = maxOf(ICON_SIZE + ICON_TOP, visibleRows * GRID_ROW_HEIGHT)
        val panel = Global.getSettings().createCustom(CONTENT_WIDTH, height, BaseCustomUIPanelPlugin())
        val icon = panel.createCustomPanel(
            ICON_SIZE,
            ICON_SIZE,
            if (weaponTile) StockReviewWeaponIconPlugin(spriteName, motifType) else IconPanelPlugin(spriteName),
        )
        panel.addComponent(icon).inTL(ICON_LEFT, minOf(ICON_TOP, maxOf(0f, height - ICON_SIZE)))

        for (i in rows.indices) {
            val row = rows[i]
            addStatRow(panel, ICON_LEFT + ICON_SIZE + ICON_GRID_GAP, i * GRID_ROW_HEIGHT, GRID_WIDTH, GRID_ROW_HEIGHT, row)
        }
        tooltip.addCustom(panel, pad)
        tooltip.addSpacer(GRID_BOTTOM_PAD)
    }

    private fun addSpecPara(tooltip: TooltipMakerAPI, text: String?, highlight: String?, pad: Float, spec: WeaponSpecAPI) {
        if (!hasText(text)) {
            return
        }
        tooltip.addSpacer(SMALL_PAD)
        val rawHighlights = splitHighlights(highlight)
        val substitutedText = substituteFormatSpecifiers(text, rawHighlights, spec)
        val displayText = truncateForLines(substitutedText, CUSTOM_TEXT_MAX_LINES, CONTENT_WIDTH)
        val highlights = visibleHighlights(displayText, rawHighlights)
        if (highlights.isNotEmpty()) {
            val label = tooltip.addPara(tooltipFormat(displayText), pad, textColor(), highlightColor(), *highlights)
            label.setHighlight(*highlights)
            label.setHighlightColor(highlightColor())
            tooltip.addSpacer(SMALL_PAD)
            return
        }
        tooltip.addPara(tooltipFormat(displayText), pad, textColor())
        tooltip.addSpacer(SMALL_PAD)
    }

    private fun addHighlightedPara(tooltip: TooltipMakerAPI, text: String, highlight: String?, pad: Float) {
        val label = tooltip.addPara(tooltipFormat(text), pad, textColor(), highlightColor(), highlight)
        label.setHighlight(highlight)
        label.setHighlightColor(highlightColor())
    }

    private fun cargoSpaceLabel(): String? {
        val cargoSpace = unitCargoSpace()
        return if (validNumber(cargoSpace)) formatOneDecimalTrim(cargoSpace) else null
    }

    private fun unitCargoSpace(): Float {
        val stocks: List<SubmarketWeaponStock> = record.submarketStocks
        for (stock in stocks) {
            val value = stock.unitCargoSpace
            if (validNumber(value) && value > 0f) {
                return value
            }
        }
        try {
            val stack = Global.getSettings().createCargoStack(CargoAPI.CargoItemType.WEAPONS, record.itemId, null)
            if (stack != null) {
                val value = stack.cargoSpacePerUnit
                if (validNumber(value) && value > 0f) {
                    return value
                }
            }
        } catch (_: RuntimeException) {
        }
        return Float.NaN
    }

    private fun priceLabel(): String? {
        var price = record.cheapestPurchasableUnitPrice
        if (price == Int.MAX_VALUE) {
            price = Math.round(maxOf(0f, record.spec?.baseValue ?: 0f))
        }
        return if (price <= 0) null else CreditFormat.credits(price)
    }

    private fun damageValue(spec: WeaponSpecAPI): String? {
        if (spec.hasTag("damage_special")) {
            return "Special"
        }
        val damage = record.damageLabel
        if (!hasMeaningful(damage)) {
            return damage
        }
        val burstSize = spec.burstSize
        if (!spec.isBeam && burstSize > 1) {
            return damage + "x" + burstSize
        }
        return damage
    }

    private fun fluxPerShotLabel(spec: WeaponSpecAPI): String {
        val projectile = projectileSpec(spec) ?: return "?"
        val energy = projectile.energyPerShot
        return if (validNumber(energy) && energy > 0f) Math.round(energy).toString() else "0"
    }

    private data class StatRow(
        val label: String = "",
        val value: String = "",
    ) {
        fun isSpacer(): Boolean = label.isEmpty() && value.isEmpty()
    }

    private class SectionHeadingPlugin : BaseCustomUIPanelPlugin() {
        private var position: PositionAPI? = null

        override fun positionChanged(position: PositionAPI?) {
            this.position = position
        }

        override fun renderBelow(alphaMult: Float) {
            val currentPosition = position ?: return
            Misc.renderQuadAlpha(
                currentPosition.x,
                currentPosition.y,
                currentPosition.width,
                currentPosition.height,
                VANILLA_SECTION,
                alphaMult,
            )
        }
    }

    private class IconPanelPlugin(private val spriteName: String?) : BaseCustomUIPanelPlugin() {
        private var position: PositionAPI? = null

        override fun positionChanged(position: PositionAPI?) {
            this.position = position
        }

        override fun render(alphaMult: Float) {
            val currentPosition = position ?: return
            val x = currentPosition.x
            val y = currentPosition.y
            val width = currentPosition.width
            val height = currentPosition.height
            renderSprite(x, y, width, height, ICON_INSET, alphaMult)
        }

        private fun renderSprite(x: Float, y: Float, width: Float, height: Float, inset: Float, alphaMult: Float) {
            val maxWidth = maxOf(1f, width - 2f * inset)
            val maxHeight = maxOf(1f, height - 2f * inset)
            renderFittedSprite(spriteName, Color.WHITE, x + width * 0.5f, y + height * 0.5f, maxWidth, maxHeight, alphaMult)
        }

        private fun renderFittedSprite(
            path: String?,
            color: Color?,
            centerX: Float,
            centerY: Float,
            maxWidth: Float,
            maxHeight: Float,
            alphaMult: Float,
        ): Boolean {
            if (!hasText(path)) {
                return false
            }
            val sprite: SpriteAPI = try {
                Global.getSettings().getSprite(path)
            } catch (_: RuntimeException) {
                return false
            } ?: return false
            if (sprite.width <= 0f || sprite.height <= 0f) {
                return false
            }
            val oldWidth = sprite.width
            val oldHeight = sprite.height
            val oldAlpha = sprite.alphaMult
            val oldColor = sprite.color
            val oldAngle = sprite.angle
            val scale = minOf(maxOf(1f, maxWidth) / oldWidth, maxOf(1f, maxHeight) / oldHeight)
            sprite.setSize(oldWidth * scale, oldHeight * scale)
            sprite.alphaMult = oldAlpha * alphaMult
            sprite.color = color ?: Color.WHITE
            sprite.angle = 0f
            sprite.renderAtCenter(centerX, centerY)
            sprite.setSize(oldWidth, oldHeight)
            sprite.alphaMult = oldAlpha
            sprite.color = oldColor
            sprite.angle = oldAngle
            return true
        }
    }

    companion object {
        private const val VANILLA_TOOLTIP_WIDTH = 400f
        private const val CONTENT_WIDTH = VANILLA_TOOLTIP_WIDTH * 1.25f
        private const val WING_TOOLTIP_WIDTH = 424f
        private const val OUTER_PAD_X = 16f
        private const val OUTER_PAD_TOP = 8f
        private const val OUTER_PAD_BOTTOM = OUTER_PAD_X
        private const val WIDTH = CONTENT_WIDTH + 2f * OUTER_PAD_X
        private const val TOOLTIP_LAYOUT_HEIGHT = 1400f
        private const val SECTION_PAD = 9f
        private const val SMALL_PAD = 4f
        private const val SECTION_CONTENT_PAD = 12f
        private const val CUSTOM_TEXT_PAD = 6f
        private const val GRID_BOTTOM_PAD = 8f
        private const val GRID_ROW_HEIGHT = 24f
        private const val SECTION_HEADING_HEIGHT = 22f
        private const val ICON_SIZE = 92f
        private const val ICON_LEFT = 28f
        private const val ICON_TOP = 12f
        private const val ICON_INSET = 2f
        private const val ICON_GRID_GAP = 44f
        private const val GRID_WIDTH = CONTENT_WIDTH - ICON_LEFT - ICON_SIZE - ICON_GRID_GAP - 8f
        private const val GRID_LABEL_WIDTH = 188f
        private val VANILLA_SECTION = Color(9, 78, 88, 225)
        private val TOOLTIP_TEXT = Color(215, 215, 215, 255)
        private val TOOLTIP_MUTED = Color(175, 175, 175, 255)
        private const val DESCRIPTION_MAX_LINES = 4
        private const val CUSTOM_TEXT_MAX_LINES = 3
        private const val ESTIMATED_DESCRIPTION_CHAR_WIDTH = 8f

        @JvmStatic
        fun forRecord(record: WeaponStockRecord?, toggleText: String?): TooltipMakerAPI.TooltipCreator? {
            if (record == null) {
                return null
            }
            if (record.isWing() && record.wingSpec == null) {
                return null
            }
            if (!record.isWing() && record.spec == null) {
                return null
            }
            return StockReviewItemTooltip(record, toggleText)
        }

        private fun addStatRow(panel: CustomPanelAPI, x: Float, y: Float, width: Float, height: Float, row: StatRow?) {
            if (row == null) {
                return
            }
            if (!hasText(row.label)) {
                addPanelLabel(panel, row.value, highlightColor(), x, y, width, height, Alignment.RMID)
                return
            }
            val valueX = x + GRID_LABEL_WIDTH
            val valueWidth = maxOf(20f, width - GRID_LABEL_WIDTH)
            addPanelLabel(panel, row.label, textColor(), x, y, GRID_LABEL_WIDTH, height, Alignment.LMID)
            addPanelLabel(panel, row.value, highlightColor(), valueX, y, valueWidth, height, Alignment.RMID)
        }

        private fun addSectionHeading(tooltip: TooltipMakerAPI, text: String, pad: Float) {
            val panel = Global.getSettings().createCustom(CONTENT_WIDTH, SECTION_HEADING_HEIGHT, SectionHeadingPlugin())
            addPanelLabel(panel, text, textColor(), 0f, 0f, CONTENT_WIDTH, SECTION_HEADING_HEIGHT, Alignment.MID)
            tooltip.addCustom(panel, pad)
        }

        private fun addPanelLabel(
            parent: CustomPanelAPI,
            text: String?,
            color: Color,
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            alignment: Alignment,
        ) {
            var labelX = x
            var labelWidth = width
            if (Alignment.LMID == alignment) {
                labelX += WimGuiStyle.TEXT_LEFT_PAD
                labelWidth = maxOf(8f, width - WimGuiStyle.TEXT_LEFT_PAD)
            }
            val label = parent.createUIElement(labelWidth, height, false)
            label.setParaFontDefault()
            label.setParaFontColor(color)
            val maxChars = WimGuiText.estimatedChars(labelWidth)
            val line: LabelAPI = label.addPara(tooltipFormat(WimGuiText.fit(text, maxChars)), 0f, color)
            line.setAlignment(alignment)
            parent.addUIElement(label).inTL(labelX, y + WimGuiStyle.TEXT_TOP_PAD)
        }

        private fun beginStyledGrid(tooltip: TooltipMakerAPI) {
            tooltip.beginGrid(WING_TOOLTIP_WIDTH, 3)
            tooltip.setGridLabelColor(textColor())
            tooltip.setGridValueColor(textColor())
        }

        private fun damageIconSpriteName(type: DamageType?): String? {
            var key = "icon_other"
            if (DamageType.KINETIC == type) {
                key = "icon_kinetic"
            } else if (DamageType.HIGH_EXPLOSIVE == type) {
                key = "icon_high_explosive"
            } else if (DamageType.FRAGMENTATION == type) {
                key = "icon_fragmentation"
            } else if (DamageType.ENERGY == type) {
                key = "icon_energy"
            }
            return try {
                Global.getSettings().getSpriteName("ui", key)
            } catch (_: RuntimeException) {
                null
            }
        }

        private fun damageLabel(spec: WeaponSpecAPI): String = if (spec.hasTag("damage_special")) "Special" else "Damage"

        private fun addMountNotes(rows: MutableList<StatRow>, spec: WeaponSpecAPI) {
            val required = requiredMountSlots(spec)
            if (hasText(required)) {
                addRow(rows, "", required)
            }
            if (spec.type != null && spec.mountType != null && spec.type != spec.mountType) {
                addRow(rows, "", "Counts as ${format(spec.type)} for stat modifiers")
            }
        }

        private fun requiredMountSlots(spec: WeaponSpecAPI): String? {
            if (spec.mountType == null || spec.type == null || spec.mountType == spec.type) {
                return null
            }
            return when (spec.mountType) {
                WeaponAPI.WeaponType.COMPOSITE -> "Requires a Ballistic, Missile, or Composite slot"
                WeaponAPI.WeaponType.HYBRID -> "Requires a Ballistic, Energy, or Hybrid slot"
                WeaponAPI.WeaponType.SYNERGY -> "Requires an Energy, Missile, or Synergy slot"
                WeaponAPI.WeaponType.UNIVERSAL -> "Requires a Ballistic, Energy, Missile, or Universal slot"
                else -> null
            }
        }

        private fun projectileSpec(spec: WeaponSpecAPI): ProjectileWeaponSpecAPI? =
            if (spec is ProjectileWeaponSpecAPI) spec else null

        private fun damageMultiplierLabel(damageType: DamageType?): String {
            if (damageType == null) {
                return "?"
            }
            val shield = Math.round(damageType.shieldMult * 100f)
            val armor = Math.round(damageType.armorMult * 100f)
            val hull = Math.round(damageType.hullMult * 100f)
            if (DamageType.KINETIC == damageType) {
                return "$shield% vs shields, $armor% vs armor"
            }
            if (DamageType.HIGH_EXPLOSIVE == damageType) {
                return "$armor% vs armor, $shield% vs shields"
            }
            if (DamageType.FRAGMENTATION == damageType) {
                return if (shield == armor) {
                    "$shield% vs shields and armor, $hull% vs hull"
                } else {
                    "$shield% vs shields, $armor% vs armor, $hull% vs hull"
                }
            }
            if (shield == 100 && armor == 100 && hull == 100) {
                return damageType.description
            }
            val parts = ArrayList<String>()
            parts.add("$shield% vs shields")
            parts.add("$armor% vs armor")
            parts.add("$hull% vs hull")
            return parts.joinToString(", ")
        }

        private fun addRow(rows: MutableList<StatRow>, label: String, value: String?) {
            if (!hasMeaningful(value)) {
                return
            }
            rows.add(StatRow(label, value ?: ""))
        }

        @Suppress("unused")
        private fun addSpacer(rows: MutableList<StatRow>) {
            if (rows.isNotEmpty() && !rows[rows.size - 1].isSpacer()) {
                rows.add(StatRow("", ""))
            }
        }

        private fun splitHighlights(highlight: String?): Array<String> {
            if (!hasText(highlight)) {
                return emptyArray()
            }
            val result = ArrayList<String>()
            for (raw in highlight!!.split("|")) {
                val trimmed = raw.trim()
                if (trimmed.isNotEmpty()) {
                    result.add(trimmed)
                }
            }
            return result.toTypedArray()
        }

        private fun titleColor(): Color = Misc.getTooltipTitleAndLightHighlightColor()

        private fun textColor(): Color = TOOLTIP_TEXT

        private fun mutedColor(): Color = TOOLTIP_MUTED

        private fun highlightColor(): Color = Misc.getHighlightColor()

        private fun setCodexEntry(tooltip: TooltipMakerAPI, entryId: String?) {
            if (hasText(entryId)) {
                tooltip.setCodexEntryId(entryId)
            }
        }

        private fun truncateDescription(text: String?): String? {
            if (!hasText(text)) {
                return text
            }
            return truncateForLines(text, DESCRIPTION_MAX_LINES, CONTENT_WIDTH)
        }

        private fun truncateForLines(text: String?, maxLines: Int, width: Float): String {
            if (!hasText(text)) {
                return text ?: ""
            }
            val normalized = text!!.trim().replace(Regex("\\s+"), " ")
            if (maxLines <= 0) {
                return normalized
            }
            var charsPerLine = maxOf(32, Math.floor((CONTENT_WIDTH / ESTIMATED_DESCRIPTION_CHAR_WIDTH).toDouble()).toInt())
            if (validNumber(width) && width > 0f) {
                charsPerLine = maxOf(32, Math.floor((width / ESTIMATED_DESCRIPTION_CHAR_WIDTH).toDouble()).toInt())
            }
            val words = normalized.split(" ")
            val result = StringBuilder(normalized.length)
            var line = 1
            var lineChars = 0
            var truncated = false
            for (word in words) {
                if (word.isEmpty()) {
                    continue
                }
                var addedChars = if (lineChars <= 0) word.length else word.length + 1
                if (lineChars > 0 && lineChars + addedChars > charsPerLine) {
                    line++
                    lineChars = 0
                    addedChars = word.length
                }
                if (line > maxLines) {
                    truncated = true
                    break
                }
                if (result.isNotEmpty()) {
                    result.append(' ')
                }
                result.append(word)
                lineChars += addedChars
            }
            if (!truncated && result.length == normalized.length) {
                return normalized
            }
            return trimForEllipsis(result.toString()) + "..."
        }

        private fun visibleHighlights(text: String?, highlights: Array<String>?): Array<String> {
            if (!hasText(text) || highlights == null || highlights.isEmpty()) {
                return emptyArray()
            }
            val result = ArrayList<String>()
            for (highlight in highlights) {
                if (hasText(highlight) && text!!.contains(highlight)) {
                    result.add(highlight)
                }
            }
            return result.toTypedArray()
        }

        private fun substituteFormatSpecifiers(text: String?, highlights: Array<String>, spec: WeaponSpecAPI): String {
            if (!hasText(text)) {
                return text ?: ""
            }
            val result = StringBuilder(text!!.length)
            var highlightIndex = 0
            var i = 0
            while (i < text.length) {
                val c = text[i]
                if (c != '%' || i + 1 >= text.length) {
                    result.append(c)
                    i++
                    continue
                }
                val next = text[i + 1]
                if (next == '%') {
                    result.append('%')
                    i += 2
                    continue
                }
                if (next == 's' || next == 'd' || next == 'f') {
                    result.append(formatHighlightValue(highlights, highlightIndex, spec))
                    highlightIndex++
                    i += 2
                    continue
                }
                result.append(c)
                i++
            }
            return result.toString()
        }

        private fun formatHighlightValue(highlights: Array<String>?, index: Int, spec: WeaponSpecAPI?): String {
            if (highlights != null && index >= 0 && index < highlights.size && hasText(highlights[index])) {
                return highlights[index].trim()
            }
            if (index == 0 && spec != null && spec.derivedStats != null) {
                val value = if (spec.isBeam) spec.derivedStats.dps else spec.derivedStats.damagePerShot
                if (validNumber(value) && value > 0f) {
                    return formatOneDecimalTrim(value)
                }
            }
            return "?"
        }

        private fun trimForEllipsis(value: String?): String {
            if (value == null) {
                return ""
            }
            var trimmed = value.trim()
            while (trimmed.endsWith(",") || trimmed.endsWith(";") || trimmed.endsWith(":") || trimmed.endsWith(".")) {
                trimmed = trimmed.substring(0, trimmed.length - 1).trim()
            }
            return trimmed
        }

        private fun tooltipFormat(value: String?): String = value?.replace("%", "%%") ?: ""

        private fun hasText(value: String?): Boolean = value != null && value.trim().isNotEmpty()

        private fun hasMeaningful(value: String?): Boolean {
            if (!hasText(value)) {
                return false
            }
            val trimmed = value!!.trim()
            return trimmed != "?" && trimmed != "---" && !trimmed.equals("None", ignoreCase = true)
        }

        private fun validNumber(value: Float): Boolean = !value.isNaN() && !value.isInfinite()

        private fun format(value: Any?): String {
            if (value == null) {
                return "?"
            }
            var text = value.toString().replace('_', ' ').trim()
            if (text.isEmpty() || text == "?") {
                return "?"
            }
            text = text.lowercase(Locale.US)
            val result = StringBuilder(text.length)
            var capitalize = true
            for (c in text) {
                if (Character.isWhitespace(c) || c == '/' || c == '-') {
                    capitalize = true
                    result.append(c)
                } else if (capitalize) {
                    result.append(c.uppercaseChar())
                    capitalize = false
                } else {
                    result.append(c)
                }
            }
            return result.toString()
        }

        private fun formatOneDecimalTrim(value: Float): String {
            if (!validNumber(value)) {
                return "?"
            }
            val rounded = Math.round(value)
            if (Math.abs(value - rounded) < 0.05f) {
                return rounded.toString()
            }
            return String.format(Locale.US, "%.1f", value)
        }
    }
}
