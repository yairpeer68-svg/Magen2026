package com.magen.family.filter;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** High-confidence guard for explicit search intent, including simple punctuation/leet evasion. */
public final class ExplicitSearchQuery {
    private static final Set<String> HIGH_CONFIDENCE = new HashSet<>(Arrays.asList(
        "porn", "pornography", "pornhub", "xvideos", "xnxx", "xhamster",
        "redtube", "youporn", "brazzers", "chaturbate", "bongacams", "pornmd",
        "nhentai", "hentai", "rule34"
    ));

    private ExplicitSearchQuery() {}

    public static boolean shouldBlock(String raw, AhoCorasick matcher, boolean useKeywords) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) return false;
        if (useKeywords && matcher != null && matcher.contains(normalized)) return true;

        String compact = compactFolded(normalized);
        if (compact.isEmpty()) return false;
        for (String term : HIGH_CONFIDENCE) {
            if (compact.equals(term) || compact.startsWith(term) || compact.contains(term)) return true;
        }
        return false;
    }

    static String normalize(String raw) {
        if (raw == null) return "";
        String n = Normalizer.normalize(raw, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(n.length());
        boolean space = false;
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append(c); space = false;
            } else if (!space && out.length() > 0) {
                out.append(' '); space = true;
            }
        }
        return out.toString().trim();
    }

    static String compactFolded(String normalized) {
        StringBuilder out = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isWhitespace(c)) continue;
            switch (c) {
                case '0': c = 'o'; break;
                case '1': c = 'i'; break;
                case '3': c = 'e'; break;
                case '4': c = 'a'; break;
                case '5': c = 's'; break;
                case '7': c = 't'; break;
                default: break;
            }
            out.append(c);
        }
        return out.toString();
    }
}
