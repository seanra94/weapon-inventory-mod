package weaponsprocurement.ui

import java.util.Locale

object WimGuiText {
    private val weakLineEndings: Set<String> = setOf(
        "a", "an", "and", "as", "at", "by", "for", "from", "in", "of", "or", "the", "to", "with",
    )

    @JvmStatic
    fun fit(text: String?, maxVisibleChars: Int): String = truncateLineToFit(text ?: "", maxVisibleChars, 10)

    @JvmStatic
    fun estimatedChars(width: Float): Int = estimatedChars(width, 0f, WimGuiStyle.TEXT_APPROX_CHAR_WIDTH)

    @JvmStatic
    fun estimatedChars(width: Float, horizontalPadding: Float, approxCharWidth: Float): Int {
        val effectiveWidth = maxOf(8f, width - maxOf(0f, horizontalPadding))
        return maxOf(1, (effectiveWidth / maxOf(1f, approxCharWidth)).toInt())
    }

    @JvmStatic
    fun wrap(text: String?, maxCharsPerLine: Int, maxLines: Int): List<String> {
        val safeChars = maxOf(1, maxCharsPerLine)
        val safeLines = maxOf(1, maxLines)
        val lines = ArrayList<String>()
        for (rawLine in (text ?: "").split(Regex("\\r?\\n"), limit = 0)) {
            lines.addAll(wrapTextLineWordAware(rawLine, safeChars))
        }
        if (lines.isEmpty()) {
            lines.add("")
        }
        if (lines.size <= safeLines) {
            return lines
        }
        val visible = ArrayList(lines.subList(0, safeLines))
        val overflow = StringBuilder()
        for (i in safeLines - 1 until lines.size) {
            if (overflow.isNotEmpty()) {
                overflow.append(' ')
            }
            overflow.append(lines[i])
        }
        visible[visible.size - 1] = fit(overflow.toString(), safeChars)
        return visible
    }

    @JvmStatic
    fun fitLayout(
        text: String?,
        availableWidth: Float,
        availableHeight: Float,
        minRowHeight: Float,
        horizontalPadding: Float,
        verticalPadding: Float,
        approxCharWidth: Float,
        lineHeight: Float,
        maxLines: Int,
        canGrowHeight: Boolean,
        maxHeight: Float,
    ): WimGuiTextLayout {
        val safeMaxLines = maxOf(1, maxLines)
        val safeLineHeight = maxOf(1f, lineHeight)
        val maxCharsPerLine = estimatedChars(availableWidth, horizontalPadding, approxCharWidth)
        var heightLimitedLines = safeMaxLines
        if (!canGrowHeight && !availableHeight.isInfinite()) {
            heightLimitedLines = maxOf(1, ((availableHeight - verticalPadding) / safeLineHeight).toInt())
        }
        val cappedMaxLines = maxOf(1, minOf(safeMaxLines, heightLimitedLines))
        val wrappedLines = wrapAll(text, maxCharsPerLine)
        val truncated = wrappedLines.size > cappedMaxLines
        val cappedLines = ArrayList<String>()
        if (truncated) {
            cappedLines.addAll(wrappedLines.subList(0, cappedMaxLines))
            val overflow = StringBuilder()
            for (i in cappedMaxLines - 1 until wrappedLines.size) {
                if (overflow.isNotEmpty()) {
                    overflow.append(' ')
                }
                overflow.append(wrappedLines[i])
            }
            cappedLines[cappedLines.size - 1] = fit(overflow.toString(), maxCharsPerLine)
        } else {
            cappedLines.addAll(wrappedLines)
        }
        if (cappedLines.isEmpty()) {
            cappedLines.add("")
        }

        val lineCount = maxOf(1, cappedLines.size)
        val desiredHeight = maxOf(minRowHeight, verticalPadding + lineCount * safeLineHeight)
        val rowHeight = if (canGrowHeight) {
            minOf(desiredHeight, maxHeight)
        } else {
            minOf(desiredHeight, availableHeight)
        }
        var widest = 0
        for (line in cappedLines) {
            widest = maxOf(widest, line.length)
        }
        return WimGuiTextLayout(
            joinLines(cappedLines),
            lineCount,
            rowHeight,
            minOf(maxOf(8f, availableWidth - horizontalPadding), widest * approxCharWidth),
            rowHeight,
            truncated,
        )
    }

    @JvmStatic
    fun fitLayout(text: String?, width: Float, minRowHeight: Float, maxLines: Int): WimGuiTextLayout = fitLayout(
        text,
        width,
        Float.POSITIVE_INFINITY,
        minRowHeight,
        WimGuiStyle.TEXT_LEFT_PAD,
        WimGuiStyle.TEXT_VERTICAL_PADDING,
        WimGuiStyle.TEXT_APPROX_CHAR_WIDTH,
        WimGuiStyle.TEXT_LINE_HEIGHT,
        maxLines,
        true,
        Float.POSITIVE_INFINITY,
    )

    private fun wrapAll(text: String?, maxCharsPerLine: Int): List<String> {
        val safeChars = maxOf(1, maxCharsPerLine)
        val lines = ArrayList<String>()
        for (rawLine in (text ?: "").split(Regex("\\r?\\n"), limit = 0)) {
            lines.addAll(wrapTextLineWordAware(rawLine, safeChars))
        }
        return if (lines.isEmpty()) listOf("") else lines
    }

    private fun wrapTextLineWordAware(line: String?, maxCharsPerLine: Int): List<String> {
        if (line == null || line.trim().isEmpty()) {
            return listOf("")
        }
        if (!line.contains(" ")) {
            return wrapLongToken(line.trim(), maxCharsPerLine)
        }

        val wrapped = ArrayList<String>()
        var current = ""
        val words = line.trim().split(Regex("\\s+"))
        for (word in words) {
            val segments = if (word.length > maxCharsPerLine) {
                wrapLongToken(word, maxCharsPerLine)
            } else {
                listOf(word)
            }
            for (segmentIndex in segments.indices) {
                val segment = segments[segmentIndex]
                current = if (current.isEmpty()) {
                    segment
                } else if (current.length + 1 + segment.length <= maxCharsPerLine) {
                    "$current $segment"
                } else {
                    wrapped.add(current)
                    segment
                }
                if (segmentIndex < segments.size - 1) {
                    wrapped.add(current)
                    current = ""
                }
            }
        }
        if (current.isNotEmpty()) {
            wrapped.add(current)
        }
        return if (wrapped.isEmpty()) listOf("") else rebalanceWeakLineEndings(wrapped, maxCharsPerLine)
    }

    private fun wrapLongToken(token: String, maxCharsPerLine: Int): List<String> {
        if (token.length <= maxCharsPerLine) {
            return listOf(token)
        }
        if (maxCharsPerLine <= 1) {
            val chars = ArrayList<String>()
            for (char in token) {
                chars.add(char.toString())
            }
            return chars
        }
        val chunks = ArrayList<String>()
        var index = 0
        while (index < token.length) {
            val end = minOf(token.length, index + maxCharsPerLine)
            chunks.add(token.substring(index, end))
            index = end
        }
        return chunks
    }

    private fun rebalanceWeakLineEndings(lines: List<String>, maxCharsPerLine: Int): List<String> {
        if (lines.size <= 1) {
            return lines
        }
        val rebalanced = ArrayList(lines)
        var index = 0
        while (index < rebalanced.size - 1) {
            val words = nonBlankWords(rebalanced[index])
            val carryCount = weakTrailingWordCount(words)
            if (carryCount > 0 && carryCount < words.size) {
                rebalanced[index] = join(words.subList(0, words.size - carryCount))
                val carried = join(words.subList(words.size - carryCount, words.size))
                rebalanced[index + 1] = "$carried ${rebalanced[index + 1]}".trim()
            }
            carryOverflowingWordsForward(rebalanced, index + 1, maxCharsPerLine)
            index++
        }
        return rebalanced
    }

    private fun carryOverflowingWordsForward(lines: MutableList<String>, startIndex: Int, maxCharsPerLine: Int) {
        var index = startIndex
        while (index < lines.size) {
            val words = nonBlankWords(lines[index])
            if (lines[index].length <= maxCharsPerLine || words.size <= 1) {
                index++
                continue
            }
            val carried = words[words.size - 1]
            lines[index] = join(words.subList(0, words.size - 1))
            if (index == lines.size - 1) {
                lines.add(carried)
            } else {
                lines[index + 1] = "$carried ${lines[index + 1]}".trim()
            }
            if (lines[index].length <= maxCharsPerLine) {
                index++
            }
        }
    }

    private fun weakTrailingWordCount(words: List<String>): Int {
        if (words.isEmpty()) {
            return 0
        }
        if (words.size >= 2 && words[words.size - 2].equals("as", ignoreCase = true)) {
            val last = stripTrailingPunctuation(words[words.size - 1])
            if (last.equals("the", ignoreCase = true) || last.equals("a", ignoreCase = true) || last.equals("an", ignoreCase = true)) {
                return 2
            }
        }
        val last = stripTrailingPunctuation(words[words.size - 1]).lowercase(Locale.ROOT)
        return if (weakLineEndings.contains(last)) 1 else 0
    }

    private fun truncateLineToFit(text: String?, maxVisibleChars: Int, longTokenBreakLength: Int): String {
        val normalized = text?.trim() ?: ""
        if (maxVisibleChars <= 0) {
            return ""
        }
        if (normalized.length <= maxVisibleChars) {
            return normalized
        }
        if (maxVisibleChars <= 3) {
            return normalized.substring(0, minOf(maxVisibleChars, normalized.length))
        }

        val words = nonBlankWords(normalized)
        if (words.size <= 1) {
            return normalized.substring(0, minOf(maxOf(1, maxVisibleChars - 3), normalized.length)) + "..."
        }

        val kept = ArrayList<String>()
        for (word in words) {
            val candidateWords = ArrayList(kept)
            candidateWords.add(word)
            val candidate = join(candidateWords)
            if (candidate.length + 4 <= maxVisibleChars) {
                kept.add(word)
                continue
            }
            if (kept.isEmpty() || word.length >= longTokenBreakLength) {
                val partial = if (kept.isEmpty()) word else join(candidateWords)
                return partial.substring(0, minOf(maxOf(1, maxVisibleChars - 3), partial.length)) + "..."
            }
            break
        }

        if (kept.isEmpty()) {
            return normalized.substring(0, minOf(maxOf(1, maxVisibleChars - 3), normalized.length)) + "..."
        }
        return join(kept) + " ..."
    }

    private fun nonBlankWords(text: String?): List<String> {
        if (text == null || text.trim().isEmpty()) {
            return emptyList()
        }
        return ArrayList(text.trim().split(Regex("\\s+")))
    }

    private fun stripTrailingPunctuation(text: String?): String = text?.replace(Regex("[,.;:]+$"), "") ?: ""

    private fun join(words: List<String>): String = words.joinToString(" ")

    private fun joinLines(lines: List<String>): String = lines.joinToString("\n")
}
