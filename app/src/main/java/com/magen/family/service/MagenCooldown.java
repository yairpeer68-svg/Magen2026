package com.magen.family.service;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * MagenCooldown — השהיה מכוונת על כיבוי/הסרת ההגנה ("cooldown").
 *
 * הרעיון המרכזי באפליקציית שמירה-עצמית: אסור שכיבוי ההגנה יהיה מיידי.
 * ברגע של חולשה קל להזין קוד ולכבות תוך שנייה. במקום זה, בקשת כיבוי/הסרה
 * *לא* מבוצעת מיד — היא מתחילה טיימר שהוגדר מראש (למשל 12 שעות), שבמהלכו:
 *   • ההגנה נשארת פעילה לגמרי,
 *   • מוצג countdown,
 *   • אפשר לבטל את הבקשה בכל רגע,
 * ורק בסופו הכיבוי/ההסרה מתאפשרים — כשרגע החולשה כבר עבר.
 *
 * המצב נשמר ב-SharedPreferences כדי לשרוד restart/reboot.
 */
public final class MagenCooldown {

    public static final String TYPE_DISABLE   = "disable";
    public static final String TYPE_UNINSTALL = "uninstall";

    private static final String PREFS = "magen_cooldown";
    private static final String KEY_TYPE   = "pending_type";
    private static final String KEY_TARGET = "target_at";
    private static final String KEY_MINUTES = "cooldown_minutes";

    /** ברירת מחדל: 12 שעות. 0 = כבוי (פעולה מיידית — תאימות לאחור). */
    public static final int DEFAULT_MINUTES = 12 * 60;
    private static final int MAX_MINUTES = 7 * 24 * 60;   // שבוע

    private MagenCooldown() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- הגדרת משך ----------

    // ── pure timing logic (Context-free → unit-testable on the JVM) ──

    public static int clampMinutes(int m) {
        return Math.max(0, Math.min(MAX_MINUTES, m));
    }

    /** מילישניות שנותרו עד targetAt, או 0 אם אין יעד/כבר עבר. */
    public static long remaining(long targetAt, long now) {
        if (targetAt <= 0L) return 0L;
        return Math.max(0L, targetAt - now);
    }

    /** קיים יעד והזמן עבר. */
    public static boolean elapsed(long targetAt, long now) {
        return targetAt > 0L && now >= targetAt;
    }

    /** קיים יעד והזמן עוד לא עבר. */
    public static boolean pending(long targetAt, long now) {
        return targetAt > 0L && now < targetAt;
    }

    /** פורמט "HH:MM:SS" מתוך מילישניות. */
    public static String formatRemaining(long ms) {
        long sec = ms / 1000L;
        long h = sec / 3600L;
        long m = (sec % 3600L) / 60L;
        long s = sec % 60L;
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    // ── Context-bound wrappers ──

    public static int configuredMinutes(Context c) {
        return clampMinutes(p(c).getInt(KEY_MINUTES, DEFAULT_MINUTES));
    }

    public static void setConfiguredMinutes(Context c, int minutes) {
        p(c).edit().putInt(KEY_MINUTES, clampMinutes(minutes)).apply();
    }

    /** האם ההשהיה פעילה בכלל (>0). אם כבויה — הקוראים מבצעים מיד. */
    public static boolean enabled(Context c) {
        return configuredMinutes(c) > 0;
    }

    // ---------- בקשה ----------

    /**
     * מבקש כיבוי/הסרה. אם כבר קיימת בקשה — משאיר אותה כמות שהיא (לא מאריך
     * ולא מקצר, כדי שלא יהיה טריק "לבקש שוב כדי לאפס"). מחזיר את הזמן שנותר.
     * אם ההשהיה כבויה — לא נוצרת בקשה (המשמעות: פעולה מיידית אצל הקורא).
     */
    public static long request(Context c, String type) {
        if (!enabled(c)) return 0L;
        SharedPreferences sp = p(c);
        long now = System.currentTimeMillis();
        long target = sp.getLong(KEY_TARGET, 0L);
        String existing = sp.getString(KEY_TYPE, "");
        if (target > now && type.equals(existing)) {
            return target - now;   // בקשה קיימת מאותו סוג — לא מאפסים
        }
        target = now + configuredMinutes(c) * 60_000L;
        sp.edit().putString(KEY_TYPE, type).putLong(KEY_TARGET, target).apply();
        return target - now;
    }

    public static void cancel(Context c) {
        p(c).edit().remove(KEY_TYPE).remove(KEY_TARGET).apply();
    }

    /** סוג הבקשה הממתינה/שהבשילה, או null אם אין בקשה כלל. */
    public static String requestType(Context c) {
        SharedPreferences sp = p(c);
        if (sp.getLong(KEY_TARGET, 0L) <= 0L) return null;
        String t = sp.getString(KEY_TYPE, "");
        return (t == null || t.isEmpty()) ? null : t;
    }

    public static boolean hasRequest(Context c, String type) {
        return type.equals(requestType(c));
    }

    /** מילישניות שנותרו עד שהבקשה תבשיל (0 אם אין בקשה או שכבר הבשילה). */
    public static long remainingMs(Context c) {
        return remaining(p(c).getLong(KEY_TARGET, 0L), System.currentTimeMillis());
    }

    /** האם קיימת בקשה מהסוג הזה שכבר הבשילה (הזמן עבר). */
    public static boolean isElapsed(Context c, String type) {
        return hasRequest(c, type) && elapsed(p(c).getLong(KEY_TARGET, 0L), System.currentTimeMillis());
    }

    /** האם קיימת בקשה מהסוג הזה שעדיין בהמתנה (הזמן לא עבר). */
    public static boolean isPending(Context c, String type) {
        return hasRequest(c, type) && pending(p(c).getLong(KEY_TARGET, 0L), System.currentTimeMillis());
    }

    /** מחרוזת קריאה של הזמן שנותר, למשל "11:59:30". */
    public static String formatRemaining(Context c) {
        return formatRemaining(remainingMs(c));
    }
}
