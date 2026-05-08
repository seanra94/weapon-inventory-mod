package weaponsprocurement.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class WimGuiText {
    private static final Set<String> WEAK_LINE_ENDINGS = new HashSet<String>(Arrays.asList(
            "a", "an", "and", "as", "at", "by", "for", "from", "in", "of", "or", "the", "to", "with"));

    private WimGuiText() {
    }

    static String fit(String text, int maxVisibleChars) {
        return truncateLineToFit(text == null ? "" : text, maxVisibleChars, 10);
    }

    static int estimatedChars(float width) {
        return estimatedChars(width, 0f, WimGuiStyle.TEXT_APPROX_CHAR_WIDTH);
    }

    static int estimatedChars(float width, float horizontalPadding, float approxCharWidth) {
        float effectiveWidth = Math.max(8f, width - Math.max(0f, horizontalPadding));
        return Math.max(1, (int) (effectiveWidth / Math.max(1f, approxCharWidth)));
    }

    static List<String> wrap(String text, int maxCharsPerLine, int maxLines) {
        int safeChars = Math.max(1, maxCharsPerLine);
        int safeLines = Math.max(1, maxLines);
        List<String> lines = new ArrayList<String>();
        String[] rawLines = (text == null ? "" : text).split("\\r?\\n", -1);
        for (int i = 0; i < rawLines.length; i++) {
            lines.addAll(wrapTextLineWordAware(rawLines[i], safeChars));
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        if (lines.size() <= safeLines) {
            return lines;
        }
        List<String> visible = new ArrayList<String>(lines.subList(0, safeLines));
        StringBuilder overflow = new StringBuilder();
        for (int i = safeLines - 1; i < lines.size(); i++) {
            if (overflow.length() > 0) {
                overflow.append(' ');
            }
            overflow.append(lines.get(i));
        }
        visible.set(visible.size() - 1, fit(overflow.toString(), safeChars));
        return visible;
    }

    static WimGuiTextLayout fitLayout(String text,
                                      float availableWidth,
                                      float availableHeight,
                                      float minRowHeight,
                                      float horizontalPadding,
                                      float verticalPadding,
                                      float approxCharWidth,
                                      float lineHeight,
                                      int maxLines,
                                      boolean canGrowHeight,
                                      float maxHeight) {
        int safeMaxLines = Math.max(1, maxLines);
        float safeLineHeight = Math.max(1f, lineHeight);
        int maxCharsPerLine = estimatedChars(availableWidth, horizontalPadding, approxCharWidth);
        int heightLimitedLines = safeMaxLines;
        if (!canGrowHeight && !Float.isInfinite(availableHeight)) {
            heightLimitedLines = Math.max(1, (int) ((availableHeight - verticalPadding) / safeLineHeight));
        }
        int cappedMaxLines = Math.max(1, Math.min(safeMaxLines, heightLimitedLines));
        List<String> wrappedLines = wrapAll(text, maxCharsPerLine);
        boolean truncated = wrappedLines.size() > cappedMaxLines;
        List<String> cappedLines = new ArrayList<String>();
        if (truncated) {
            cappedLines.addAll(wrappedLines.subList(0, cappedMaxLines));
            StringBuilder overflow = new StringBuilder();
            for (int i = cappedMaxLines - 1; i < wrappedLines.size(); i++) {
                if (overflow.length() > 0) {
                    overflow.append(' ');
                }
                overflow.append(wrappedLines.get(i));
            }
            cappedLines.set(cappedLines.size() - 1, fit(overflow.toString(), maxCharsPerLine));
        } else {
            cappedLines.addAll(wrappedLines);
        }
        if (cappedLines.isEmpty()) {
            cappedLines.add("");
        }

        int lineCount = Math.max(1, cappedLines.size());
        float desiredHeight = Math.max(minRowHeight, verticalPadding + lineCount * safeLineHeight);
        float rowHeight = canGrowHeight
                ? Math.min(desiredHeight, maxHeight)
                : Math.min(desiredHeight, availableHeight);
        int widest = 0;
        for (int i = 0; i < cappedLines.size(); i++) {
            widest = Math.max(widest, cappedLines.get(i).length());
        }
        return new WimGuiTextLayout(
                joinLines(cappedLines),
                lineCount,
                rowHeight,
                Math.min(Math.max(8f, availableWidth - horizontalPadding), widest * approxCharWidth),
                rowHeight,
                truncated);
    }

    static WimGuiTextLayout fitLayout(String text, float width, float minRowHeight, int maxLines) {
        return fitLayout(
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
                Float.POSITIVE_INFINITY);
    }

    private static List<String> wrapAll(String text, int maxCharsPerLine) {
        int safeChars = Math.max(1, maxCharsPerLine);
        List<String> lines = new ArrayList<String>();
        String[] rawLines = (text == null ? "" : text).split("\\r?\\n", -1);
        for (int i = 0; i < rawLines.length; i++) {
            lines.addAll(wrapTextLineWordAware(rawLines[i], safeChars));
        }
        return lines.isEmpty() ? Collections.singletonList("") : lines;
    }

    private static List<String> wrapTextLineWordAware(String line, int maxCharsPerLine) {
        if (line == null || line.trim().isEmpty()) {
            return Collections.singletonList("");
        }
        if (!line.contains(" ")) {
            return wrapLongToken(line.trim(), maxCharsPerLine);
        }

        List<String> wrapped = new ArrayList<String>();
        String current = "";
        String[] words = line.trim().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            List<String> segments = words[i].length() > maxCharsPerLine
                    ? wrapLongToken(words[i], maxCharsPerLine)
                    : Collections.singletonList(words[i]);
            for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
                String segment = segments.get(segmentIndex);
                if (current.isEmpty()) {
                    current = segment;
                } else if (current.length() + 1 + segment.length() <= maxCharsPerLine) {
                    current = current + " " + segment;
                } else {
                    wrapped.add(current);
                    current = segment;
                }
                if (segmentIndex < segments.size() - 1) {
                    wrapped.add(current);
                    current = "";
                }
            }
        }
        if (!current.isEmpty()) {
            wrapped.add(current);
        }
        return wrapped.isEmpty() ? Collections.singletonList("") : rebalanceWeakLineEndings(wrapped, maxCharsPerLine);
    }

    private static List<String> wrapLongToken(String token, int maxCharsPerLine) {
        if (token.length() <= maxCharsPerLine) {
            return Collections.singletonList(token);
        }
        if (maxCharsPerLine <= 1) {
            List<String> chars = new ArrayList<String>();
            for (int i = 0; i < token.length(); i++) {
                chars.add(String.valueOf(token.charAt(i)));
            }
            return chars;
        }
        List<String> chunks = new ArrayList<String>();
        int index = 0;
        while (index < token.length()) {
            int end = Math.min(token.length(), index + maxCharsPerLine);
            chunks.add(token.substring(index, end));
            index = end;
        }
        return chunks;
    }

    private static List<String> rebalanceWeakLineEndings(List<String> lines, int maxCharsPerLine) {
        if (lines.size() <= 1) {
            return lines;
        }
        List<String> rebalanced = new ArrayList<String>(lines);
        int index = 0;
        while (index < rebalanced.size() - 1) {
            List<String> words = nonBlankWords(rebalanced.get(index));
            int carryCount = weakTrailingWordCount(words);
            if (carryCount > 0 && carryCount < words.size()) {
                rebalanced.set(index, join(words.subList(0, words.size() - carryCount)));
                String carried = join(words.subList(words.size() - carryCount, words.size()));
                rebalanced.set(index + 1, (carried + " " + rebalanced.get(index + 1)).trim());
            }
            carryOverflowingWordsForward(rebalanced, index + 1, maxCharsPerLine);
            index++;
        }
        return rebalanced;
    }

    private static void carryOverflowingWordsForward(List<String> lines, int startIndex, int maxCharsPerLine) {
        int index = startIndex;
        while (index < lines.size()) {
            List<String> words = nonBlankWords(lines.get(index));
            if (lines.get(index).length() <= maxCharsPerLine || words.size() <= 1) {
                index++;
                continue;
            }
            String carried = words.get(words.size() - 1);
            lines.set(index, join(words.subList(0, words.size() - 1)));
            if (index == lines.size() - 1) {
                lines.add(carried);
            } else {
                lines.set(index + 1, (carried + " " + lines.get(index + 1)).trim());
            }
            if (lines.get(index).length() <= maxCharsPerLine) {
                index++;
            }
        }
    }

    private static int weakTrailingWordCount(List<String> words) {
        if (words.isEmpty()) {
            return 0;
        }
        if (words.size() >= 2 && "as".equalsIgnoreCase(words.get(words.size() - 2))) {
            String last = stripTrailingPunctuation(words.get(words.size() - 1));
            if ("the".equalsIgnoreCase(last) || "a".equalsIgnoreCase(last) || "an".equalsIgnoreCase(last)) {
                return 2;
            }
        }
        String last = stripTrailingPunctuation(words.get(words.size() - 1)).toLowerCase();
        return WEAK_LINE_ENDINGS.contains(last) ? 1 : 0;
    }

    private static String truncateLineToFit(String text, int maxVisibleChars, int longTokenBreakLength) {
        String normalized = text == null ? "" : text.trim();
        if (maxVisibleChars <= 0) {
            return "";
        }
        if (normalized.length() <= maxVisibleChars) {
            return normalized;
        }
        if (maxVisibleChars <= 3) {
            return normalized.substring(0, Math.min(maxVisibleChars, normalized.length()));
        }

        List<String> words = nonBlankWords(normalized);
        if (words.size() <= 1) {
            return normalized.substring(0, Math.min(Math.max(1, maxVisibleChars - 3), normalized.length())) + "...";
        }

        List<String> kept = new ArrayList<String>();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            List<String> candidateWords = new ArrayList<String>(kept);
            candidateWords.add(word);
            String candidate = join(candidateWords);
            if (candidate.length() + 4 <= maxVisibleChars) {
                kept.add(word);
                continue;
            }
            if (kept.isEmpty() || word.length() >= longTokenBreakLength) {
                String partial = kept.isEmpty() ? word : join(candidateWords);
                return partial.substring(0, Math.min(Math.max(1, maxVisibleChars - 3), partial.length())) + "...";
            }
            break;
        }

        if (kept.isEmpty()) {
            return normalized.substring(0, Math.min(Math.max(1, maxVisibleChars - 3), normalized.length())) + "...";
        }
        return join(kept) + " ...";
    }

    private static List<String> nonBlankWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(Arrays.asList(text.trim().split("\\s+")));
    }

    private static String stripTrailingPunctuation(String text) {
        return text == null ? "" : text.replaceAll("[,.;:]+$", "");
    }

    private static String join(List<String> words) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(words.get(i));
        }
        return result.toString();
    }

    private static String joinLines(List<String> lines) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(lines.get(i));
        }
        return result.toString();
    }
}
