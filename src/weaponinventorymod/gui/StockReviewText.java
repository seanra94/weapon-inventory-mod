package weaponinventorymod.gui;

import java.util.ArrayList;
import java.util.List;

final class StockReviewText {
    private StockReviewText() {
    }

    static String fit(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim();
        if (maxChars <= 0 || normalized.length() <= maxChars) {
            return normalized;
        }
        if (maxChars <= 3) {
            return normalized.substring(0, Math.min(maxChars, normalized.length()));
        }
        int hardLimit = Math.max(1, maxChars - 3);
        String candidate = normalized.substring(0, Math.min(hardLimit, normalized.length())).trim();
        int lastSpace = candidate.lastIndexOf(' ');
        if (lastSpace >= hardLimit / 2) {
            String wholeWord = candidate.substring(0, lastSpace).trim();
            if (!wholeWord.isEmpty()) {
                return wholeWord + " ...";
            }
        }
        return candidate + "...";
    }

    static List<String> wrap(String text, int maxCharsPerLine, int maxLines) {
        int safeChars = Math.max(1, maxCharsPerLine);
        int safeLines = Math.max(1, maxLines);
        List<String> lines = wrapLine(text == null ? "" : text, safeChars);
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

    private static List<String> wrapLine(String text, int maxCharsPerLine) {
        List<String> result = new ArrayList<String>();
        if (text == null || text.trim().isEmpty()) {
            result.add("");
            return result;
        }
        String[] words = text.trim().split("\\s+");
        String current = "";
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (current.isEmpty()) {
                current = word;
            } else if (current.length() + 1 + word.length() <= maxCharsPerLine) {
                current = current + " " + word;
            } else {
                result.add(current);
                current = word;
            }
        }
        if (!current.isEmpty()) {
            result.add(current);
        }
        return result.isEmpty() ? java.util.Collections.singletonList("") : result;
    }
}
