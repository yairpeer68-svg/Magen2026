package com.magen.family.visual;

/** Pure package-level routing for visual enforcement surfaces. */
public final class VisualSurfacePolicy {
    private VisualSurfacePolicy() {}

    /**
     * Surfaces where an unsafe picture should be masked in-place rather than closing the app.
     * Explicit adult sites are still rejected earlier by URL/domain/text policy.
     */
    public static boolean isRegionMaskSurface(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        String p = pkg.toLowerCase(java.util.Locale.ROOT);
        return p.equals("com.google.android.youtube") ||
               p.equals("com.google.android.googlequicksearchbox") ||
               p.contains("chrome") || p.contains("browser") ||
               p.contains("firefox") || p.contains("opera") ||
               p.equals("com.microsoft.emmx") ||
               p.equals("com.brave.browser") ||
               p.equals("com.sec.android.app.sbrowser");
    }
}
