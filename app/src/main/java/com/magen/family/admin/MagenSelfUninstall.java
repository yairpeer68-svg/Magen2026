package com.magen.family.admin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import com.magen.family.service.FilterService;
import com.magen.family.service.FloatingBadgeService;
import com.magen.family.service.MagenKillSwitch;
import com.magen.family.service.MagenVpnService;

import com.magen.family.MagenApp;
import com.magen.family.service.MagenGuard;

/**
 * הסרה-עצמית מבוקרת ("כפתור הסר את האפליקציה").
 *
 * מטרה: לתת נתיב *נקי ואוטומטי* להסרת האפליקציה אחרי אימות ההורה (PIN),
 * במקום להיאבק ידנית בכיבוי מנהל-מכשיר → נגישות → הסרה. לחיצה אחת:
 *   1. מסמנת "הסרה בתהליך" — כדי ששכבות ההגנה לא ינעלו את המסך או יחסמו
 *      את דיאלוג ההסרה בדיוק כשמסירים (ראה inProgress() בשומרים).
 *   2. מכבה את סינון התוכן ועוצרת את שירותי ההגנה (VPN, מדבקה צפה, KillSwitch).
 *   3. מסירה את מנהל-המכשיר — אחרת אנדרואיד חוסם הסרה של אפליקציית-admin
 *      פעילה ("זו אפליקציית ניהול מכשיר").
 *   4. פותחת את דיאלוג ההסרה הרגיל של המערכת (אישור אחד של המשתמש).
 *
 * הערה: בלי Device Owner אי אפשר הסרה "שקטה" לגמרי — המערכת תמיד מבקשת אישור
 * אחד. הכפתור הזה עושה את כל השאר אוטומטית, כך שנשאר בדיוק אישור מערכת אחד.
 */
public final class MagenSelfUninstall {

    private static final String TAG = "MagenSelfUninstall";
    private static final String PREFS = "magen_self_uninstall";
    private static final String KEY_UNTIL = "in_progress_until";
    /** חלון זמן מספיק להשלים את דיאלוג ההסרה בלי שההגנה תתערב. */
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    private MagenSelfUninstall() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** האם הסרה-עצמית מכוונת מתבצעת כרגע. נבדק ע"י שכבות ההגנה. */
    public static boolean inProgress(Context c) {
        try {
            return System.currentTimeMillis() < prefs(c).getLong(KEY_UNTIL, 0L);
        } catch (Exception e) {
            return false;
        }
    }

    public static void arm(Context c) {
        prefs(c).edit().putLong(KEY_UNTIL, System.currentTimeMillis() + WINDOW_MS).apply();
    }

    public static void cancel(Context c) {
        prefs(c).edit().remove(KEY_UNTIL).apply();
    }

    /**
     * מריצה את זרימת ההסרה המלאה. יש לקרוא רק אחרי אימות PIN של ההורה.
     * הקריאה חייבת להגיע מ-Activity גלויה (דיאלוג ההסרה נפתח מתוכה).
     */
    public static void start(Activity activity) {
        if (activity == null) return;
        Context app = activity.getApplicationContext();

        // 1. סמן "הסרה בתהליך" — משתיק את כל נתיבי הנעילה/החסימה למשך החלון.
        arm(app);

        // מותר לשומר-הנגישות לתת למסך מידע-האפליקציה/ההסרה לעבור.
        try { MagenGuard.grantMaintenance(app, MagenGuard.SCOPE_APP_DETAILS); } catch (Exception ignored) {}

        // 2. כבה סינון תוכן (מונע חסימות תוך כדי המעבר להגדרות/ל-installer).
        try {
            MagenApp.getInstance().getPrefs().edit()
                .putBoolean(MagenApp.KEY_FILTER_ENABLED, false).apply();
        } catch (Exception e) {
            Log.w(TAG, "disable filter: " + e.getMessage());
        }

        // 3. עצור את שירותי ההגנה כדי שלא יקימו overlay/נעילה בזמן ההסרה.
        stopProtectionServices(app);

        // 4. הסר את מנהל-המכשיר — אחרת ההסרה נחסמת ע"י המערכת.
        try {
            MagenDeviceAdmin.removeAdmin(app);
        } catch (Exception e) {
            Log.w(TAG, "removeAdmin: " + e.getMessage());
        }

        // 5. פתח את דיאלוג ההסרה של המערכת (אישור אחד).
        launchUninstall(activity);
    }

    private static void stopProtectionServices(Context app) {
        stopQuietly(app, MagenVpnService.class);
        stopQuietly(app, FloatingBadgeService.class);
        stopQuietly(app, MagenKillSwitch.class);
        stopQuietly(app, FilterService.class);
    }

    private static void stopQuietly(Context app, Class<?> cls) {
        try {
            app.stopService(new Intent(app, cls));
        } catch (Throwable t) {
            Log.w(TAG, "stop " + cls.getSimpleName() + ": " + t.getClass().getSimpleName());
        }
    }

    private static void launchUninstall(Activity activity) {
        String pkg = activity.getPackageName();
        // ACTION_DELETE הוא הנתיב היציב ביותר בין יצרנים ומציג את דיאלוג
        // ההסרה הסטנדרטי. חייב NEW_TASK כי הוא יוצא מה-task של האפליקציה.
        try {
            Intent i = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            return;
        } catch (Exception primary) {
            Log.w(TAG, "ACTION_DELETE failed: " + primary.getClass().getSimpleName());
        }
        // fallback: ACTION_UNINSTALL_PACKAGE
        try {
            Intent i = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + pkg));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
        } catch (Exception fallback) {
            Log.e(TAG, "uninstall launch failed: " + fallback.getClass().getSimpleName());
        }
    }
}
