package com.magen.family.util;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.magen.family.admin.MagenDeviceAdmin;

/** Central source of truth for every Android capability Magen actually uses. */
public final class PermissionMatrix {
    public static final int SCHEMA_VERSION = 2;
    private PermissionMatrix() {}

    public static boolean notifications(Context c) {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(c,
            android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean overlay(Context c) { return Settings.canDrawOverlays(c); }
    public static boolean vpn(Context c) { return VpnService.prepare(c) == null; }
    public static boolean fineLocation(Context c) { return ContextCompat.checkSelfPermission(c,
        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED; }
    public static boolean backgroundLocation(Context c) { return Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(c,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED; }
    public static boolean usage(Context c) {
        try {
            AppOpsManager a=(AppOpsManager)c.getSystemService(Context.APP_OPS_SERVICE);
            return a!=null && a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), c.getPackageName())==AppOpsManager.MODE_ALLOWED;
        } catch(Exception e){ return false; }
    }
    public static boolean batteryExempt(Context c) {
        try { PowerManager p=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
            return p!=null && p.isIgnoringBatteryOptimizations(c.getPackageName()); }
        catch(Exception e){ return false; }
    }
    public static boolean admin(Context c) { return MagenDeviceAdmin.isAdminActive(c); }
    public static boolean accessibility(Context c) { return AccessibilityState.isMagenEnabled(c); }
    public static boolean canInstallUpdates(Context c) {
        return Build.VERSION.SDK_INT < 26 || c.getPackageManager().canRequestPackageInstalls();
    }
    public static boolean deviceOwner(Context c) {
        try { DevicePolicyManager d=(DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);
            return d!=null && d.isDeviceOwnerApp(c.getPackageName()); }
        catch(Exception e){ return false; }
    }

    /** Core capabilities that must be present before protection is considered complete. */
    public static boolean coreReady(Context c) {
        return overlay(c) && vpn(c) && admin(c) && accessibility(c);
    }

    /** All user-grantable capabilities used by enabled features. Optional capabilities are included. */
    public static int grantedCount(Context c) {
        boolean[] v={notifications(c),overlay(c),vpn(c),usage(c),fineLocation(c),backgroundLocation(c),
            batteryExempt(c),canInstallUpdates(c),admin(c),accessibility(c)};
        int n=0; for(boolean b:v) if(b)n++; return n;
    }
    public static int totalCount(Context c) { return Build.VERSION.SDK_INT>=29 ? 10 : 9; }

    public static Intent unknownSourcesIntent(Context c) {
        return new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:"+c.getPackageName()));
    }
}
