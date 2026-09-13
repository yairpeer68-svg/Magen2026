package com.magen.vpnprobe;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final int REQ_VPN = 9001;
    private TextView report;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);

        TextView title = new TextView(this);
        title.setText("VPN Probe — Android consent test");
        title.setTextSize(24f);
        root.addView(title);

        Button request = new Button(this);
        request.setText("REQUEST VPN PERMISSION");
        request.setOnClickListener(v -> requestVpn());
        root.addView(request);

        Button refresh = new Button(this);
        refresh.setText("REFRESH DIAGNOSTICS");
        refresh.setOnClickListener(v -> showReport("manual refresh"));
        root.addView(refresh);

        Button copy = new Button(this);
        copy.setText("COPY REPORT");
        copy.setOnClickListener(v -> copyReport());
        root.addView(copy);

        report = new TextView(this);
        report.setTextSize(14f);
        report.setTextIsSelectable(true);
        report.setPadding(0, 24, 0, 80);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(report);
        root.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        showReport("app opened");
    }

    private void requestVpn() {
        StringBuilder pre = diagnostics("before request");
        try {
            Intent intent = VpnService.prepare(this);
            if (intent == null) {
                pre.append("\nREQUEST: prepare() == null -> already authorized\n");
                report.setText(pre.toString());
                Toast.makeText(this, "Already authorized by Android", Toast.LENGTH_LONG).show();
                return;
            }
            pre.append("\nREQUEST: launching Android consent activity\n");
            report.setText(pre.toString());
            startActivityForResult(intent, REQ_VPN);
        } catch (Throwable t) {
            pre.append("\nREQUEST EXCEPTION: ").append(t.getClass().getName())
                .append(": ").append(t.getMessage()).append('\n');
            report.setText(pre.toString());
        }
    }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req != REQ_VPN) return;
        StringBuilder sb = diagnostics("after Android VPN consent result");
        sb.append("\nRESULT_CODE: ").append(result)
          .append(result == RESULT_OK ? " (RESULT_OK)" : result == RESULT_CANCELED ? " (RESULT_CANCELED)" : "")
          .append('\n');
        try {
            sb.append("AUTHORIZED_AFTER_RESULT: ")
              .append(VpnService.prepare(this) == null).append('\n');
        } catch (Throwable t) {
            sb.append("POST_CHECK_EXCEPTION: ").append(t).append('\n');
        }
        report.setText(sb.toString());
    }

    private void showReport(String reason) { report.setText(diagnostics(reason).toString()); }

    private StringBuilder diagnostics(String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("VPN PROBE REPORT\n================\n");
        sb.append("reason: ").append(reason).append('\n');
        sb.append("package: ").append(getPackageName()).append('\n');
        sb.append("device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        sb.append("android: ").append(Build.VERSION.RELEASE).append(" API ").append(Build.VERSION.SDK_INT).append('\n');

        UserManager um = (UserManager) getSystemService(USER_SERVICE);
        if (um != null) {
            sb.append("managed_profile: ").append(um.isManagedProfile()).append('\n');
            if (Build.VERSION.SDK_INT >= 24) sb.append("system_user: ").append(um.isSystemUser()).append('\n');
            sb.append("DISALLOW_CONFIG_VPN: ").append(um.hasUserRestriction(UserManager.DISALLOW_CONFIG_VPN)).append('\n');
            try {
                Bundle restrictions = um.getUserRestrictions();
                List<String> enabled = new ArrayList<>();
                for (String k : restrictions.keySet()) if (restrictions.getBoolean(k, false)) enabled.add(k);
                Collections.sort(enabled);
                sb.append("active_user_restrictions: ").append(enabled.isEmpty() ? "none" : enabled).append('\n');
            } catch (Throwable t) { sb.append("user_restrictions_error: ").append(t).append('\n'); }
        }

        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            sb.append("this_app_device_owner: ").append(dpm.isDeviceOwnerApp(getPackageName())).append('\n');
            sb.append("this_app_profile_owner: ").append(dpm.isProfileOwnerApp(getPackageName())).append('\n');
            try {
                List<ComponentName> admins = dpm.getActiveAdmins();
                sb.append("active_admin_count: ").append(admins == null ? 0 : admins.size()).append('\n');
                if (admins != null) for (ComponentName c : admins) sb.append("  admin: ").append(c.flattenToShortString()).append('\n');
            } catch (Throwable t) { sb.append("active_admins_error: ").append(t).append('\n'); }
        }

        try {
            String alwaysOn = Settings.Secure.getString(getContentResolver(), "always_on_vpn_app");
            int lockdown = Settings.Secure.getInt(getContentResolver(), "always_on_vpn_lockdown", 0);
            sb.append("secure_always_on_vpn_app: ").append(alwaysOn == null ? "<none>" : alwaysOn).append('\n');
            sb.append("secure_always_on_vpn_lockdown: ").append(lockdown).append('\n');
        } catch (Throwable t) { sb.append("secure_vpn_settings_error: ").append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append('\n'); }

        try {
            Intent prep = VpnService.prepare(this);
            sb.append("prepare_returns_null: ").append(prep == null).append('\n');
            if (prep != null) {
                sb.append("prepare_action: ").append(prep.getAction()).append('\n');
                sb.append("prepare_component: ").append(prep.getComponent()).append('\n');
                sb.append("prepare_package: ").append(prep.getPackage()).append('\n');
                ResolveInfo ri = getPackageManager().resolveActivity(prep, PackageManager.MATCH_DEFAULT_ONLY);
                if (ri != null && ri.activityInfo != null) {
                    sb.append("resolved_consent_activity: ").append(ri.activityInfo.packageName)
                      .append('/').append(ri.activityInfo.name).append('\n');
                    sb.append("resolved_activity_exported: ").append(ri.activityInfo.exported).append('\n');
                    sb.append("resolved_activity_enabled: ").append(ri.activityInfo.enabled).append('\n');
                } else {
                    sb.append("resolved_consent_activity: <NONE>\n");
                }
            }
        } catch (Throwable t) {
            sb.append("prepare_exception: ").append(t.getClass().getName()).append(": ").append(t.getMessage()).append('\n');
        }
        return sb;
    }

    private void copyReport() {
        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(android.content.ClipData.newPlainText("VPN Probe report", report.getText()));
        Toast.makeText(this, "Report copied", Toast.LENGTH_SHORT).show();
    }
}
