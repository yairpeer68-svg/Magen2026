package com.magen.family.service.vpn;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Central network-protection policy.
 *
 * v4.5.1 security rule: FULL_TUNNEL is the production default and is migrated
 * on upgrade. DNS-only mode cannot capture arbitrary resolver IPs and therefore
 * must not be user-selectable in the production UI.
 *
 * The engine may request a restart when it encounters repeated packet errors,
 * but it does not silently downgrade to DNS-only. That keeps the protection
 * fail-closed instead of reopening the arbitrary-DNS bypass.
 */
public final class VpnPolicy {

    private static final String PREFS = "magen_vpn_policy";
    private static final String K_FULL_TUNNEL = "full_tunnel";
    private static final String K_POLICY_VERSION = "policy_version";
    private static final int POLICY_VERSION = 3;
    private static final String K_BLOCK_QUIC  = "block_quic";
    private static final String K_SNI_FILTER  = "sni_filter";
    private static final String K_UPSTREAM    = "upstream_dns";
    private static final String K_BLOCK_HOTSPOT = "block_hotspot";

    public static final String DEFAULT_UPSTREAM_DNS = "1.1.1.3";
    public static final String FALLBACK_UPSTREAM_DNS = "1.0.0.3";
    private static final String LEGACY_DEFAULT_UPSTREAM_DNS = "94.140.14.15";

    /**
     * שרשרת resolver-ים לגיבוי (failover). כולם מסננים תוכן למבוגרים (family-safe),
     * ולכן מעבר ביניהם *אינו* פותח עקיפה — הוא רק מונע "אין אינטרנט" כשספק אחד
     * אינו נגיש ברשת מסוימת. קודם כל שאילתות ה-DNS הלכו ל-resolver יחיד בלי גיבוי
     * אמיתי; אם הוא נחסם/איטי, כל פתרון-השמות נפל וכל האינטרנט "מת".
     */
    public static final String[] FAMILY_RESOLVERS = {
        "1.1.1.3",         // Cloudflare for Families (malware + adult)
        "1.0.0.3",         // Cloudflare for Families (secondary)
        "94.140.14.15",    // AdGuard Family
        "94.140.15.16",    // AdGuard Family secondary
        "208.67.222.123",  // OpenDNS FamilyShield
        "208.67.220.123"   // OpenDNS FamilyShield secondary
    };

    /** ה-resolver המוגדר קודם, ואז שאר ה-family resolvers כגיבוי — בלי כפילויות. */
    public static java.util.List<String> upstreamDnsChain() {
        java.util.LinkedHashSet<String> ordered = new java.util.LinkedHashSet<>();
        String primary = upstreamDns();
        if (primary != null && !primary.isEmpty()) ordered.add(primary);
        for (String r : FAMILY_RESOLVERS) ordered.add(r);
        return new java.util.ArrayList<>(ordered);
    }

    private static volatile SharedPreferences prefs;
    private static volatile boolean fullTunnelRuntime = true;

    private VpnPolicy() {}

    public static synchronized void init(Context ctx) {
        if (prefs == null) {
            prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
        int version = prefs.getInt(K_POLICY_VERSION, 0);
        if (version < POLICY_VERSION) {
            // Security migration from releases where full tunnel defaulted OFF.
            SharedPreferences.Editor migration = prefs.edit()
                .putBoolean(K_FULL_TUNNEL, true)
                .putBoolean(K_BLOCK_QUIC, true)
                .putBoolean(K_SNI_FILTER, true)
                .putInt(K_POLICY_VERSION, POLICY_VERSION);
            String configuredDns = prefs.getString(K_UPSTREAM, "");
            if (configuredDns == null || configuredDns.isEmpty() ||
                    LEGACY_DEFAULT_UPSTREAM_DNS.equals(configuredDns)) {
                migration.putString(K_UPSTREAM, DEFAULT_UPSTREAM_DNS);
            }
            migration.apply();
        }
        // Production policy is fail-closed. Keep this true even if a stale
        // preference from an older build still says false.
        fullTunnelRuntime = true;
    }

    public static boolean fullTunnel() { return fullTunnelRuntime; }

    /**
     * Kept for source compatibility with old UI/backups. Production builds do
     * not allow downgrading to DNS-only because that reopens arbitrary DNS.
     */
    public static void setFullTunnel(Context ctx, boolean enabled) {
        init(ctx);
        fullTunnelRuntime = true;
        prefs.edit().putBoolean(K_FULL_TUNNEL, true).putInt(K_POLICY_VERSION, POLICY_VERSION).apply();
    }

    /** Legacy entry point: no longer downgrades protection. */
    public static void disableFullTunnelForSession() {
        fullTunnelRuntime = true;
    }

    public static boolean blockQuic() {
        return prefs == null || prefs.getBoolean(K_BLOCK_QUIC, true);
    }

    public static void setBlockQuic(Context ctx, boolean enabled) {
        init(ctx);
        // QUIC bypasses TCP SNI inspection; production policy keeps it blocked.
        prefs.edit().putBoolean(K_BLOCK_QUIC, true).apply();
    }

    public static boolean sniFilter() {
        return prefs == null || prefs.getBoolean(K_SNI_FILTER, true);
    }

    public static void setSniFilter(Context ctx, boolean enabled) {
        init(ctx);
        prefs.edit().putBoolean(K_SNI_FILTER, true).apply();
    }

    public static boolean blockEch() {
        return prefs == null || prefs.getBoolean("block_ech", true);
    }

    public static boolean blockHotspot() {
        return prefs != null && prefs.getBoolean(K_BLOCK_HOTSPOT, false);
    }

    public static void setBlockHotspot(Context ctx, boolean enabled) {
        init(ctx);
        prefs.edit().putBoolean(K_BLOCK_HOTSPOT, enabled).apply();
    }

    public static String upstreamDns() {
        return prefs == null ? DEFAULT_UPSTREAM_DNS
            : prefs.getString(K_UPSTREAM, DEFAULT_UPSTREAM_DNS);
    }

    public static void setUpstreamDns(Context ctx, String ip) {
        init(ctx);
        prefs.edit().putString(K_UPSTREAM, ip).apply();
    }
}
