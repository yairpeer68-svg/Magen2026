package com.magen.family.filter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** High-confidence fallback when a browser/custom-tab does not expose a usable URL. */
public final class BrowserExplicitContent {
    private BrowserExplicitContent() {}

    private static final Pattern STRONG = Pattern.compile(
        "(^|[^\\p{L}\\p{N}])(porn|pornography|pornhub|xvideos|xnxx|xhamster|hentai|" +
        "blowjob|creampie|deepthroat|gangbang|chaturbate|spankbang|brazzers|rule34)" +
        "([^\\p{L}\\p{N}]|$)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern AMBIGUOUS = Pattern.compile(
        "(^|[^\\p{L}\\p{N}])(sex|nude|naked|erotic|nsfw|hardcore|milf|bdsm|" +
        "pussy|cock|tits|boobs|orgasm|dildo|vibrator|escort)" +
        "([^\\p{L}\\p{N}]|$)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static boolean isClearlyExplicit(String visibleText) {
        if (visibleText == null) return false;
        String text = visibleText.toLowerCase(Locale.ROOT).trim();
        if (text.isEmpty()) return false;
        if (STRONG.matcher(text).find()) return true;

        // Ambiguous words require repeated evidence. This avoids blocking a benign page
        // merely because it mentions a single health/education term once.
        Matcher m = AMBIGUOUS.matcher(text);
        int hits = 0;
        while (m.find()) {
            if (++hits >= 2) return true;
        }
        return false;
    }
}
