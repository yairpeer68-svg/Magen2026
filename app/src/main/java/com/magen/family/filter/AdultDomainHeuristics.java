package com.magen.family.filter;

/**
 * High-confidence local adult-domain signals used before DNS is forwarded upstream.
 * Keep this conservative: the remote blocklist and family DNS handle broad coverage,
 * while these rules are the fail-safe for obvious/mirror domains that are still new.
 */
public final class AdultDomainHeuristics {
    private AdultDomainHeuristics() {}

    private static final String[] STRONG_LABEL_TOKENS = {
        "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn",
        "spankbang", "brazzers", "chaturbate", "stripchat", "fapello", "fapster",
        "porn", "xxx", "hentai", "sexcam", "camsex", "escort", "rule34", "nsfw"
    };

    public static boolean isClearlyAdultHost(String host) {
        String h = HostUtil.normalizeHost(host);
        if (h.isEmpty() || h.indexOf(':') >= 0 || h.matches("\\d{1,3}(?:\\.\\d{1,3}){3}")) {
            return false;
        }

        String[] labels = h.split("\\.");
        // Never inspect only the first label: CDN/adult mirrors often place the signal in
        // a subdomain. The TLD itself is intentionally skipped.
        for (int i = 0; i < Math.max(0, labels.length - 1); i++) {
            String label = labels[i];
            if (label.isEmpty()) continue;
            for (String token : STRONG_LABEL_TOKENS) {
                if (label.contains(token)) return true;
            }

            // "fap" is strong adult slang but also prefixes legitimate words such as
            // "fapiao". Restrict it to a whole label or a suffix so fakfap/myfap are caught
            // without turning every fap... domain into a false positive.
            if (label.equals("fap") || label.endsWith("-fap") || label.endsWith("fap")) {
                return true;
            }
        }
        return false;
    }
}
