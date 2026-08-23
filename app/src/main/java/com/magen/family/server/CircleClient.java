package com.magen.family.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.magen.family.service.NotificationHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CircleClient — client for "מעגל חיזוק" (anonymous peer encouragement).
 *
 * All calls go through the pinned, device-signed {@link MagenApiClient}. The user is anonymous to
 * peers; the server links only a one-way hash. Opt-in state is mirrored locally so a non-participant
 * makes zero Circle network calls (privacy + battery), and so the notifications poll can be skipped
 * entirely when the user is not in the circle.
 */
public final class CircleClient {
    private static final String TAG = "MagenCircle";
    private static final String PREFS = "magen_circle";
    private static final String K_RECEIVE = "opt_receive";
    private static final String K_HELP    = "opt_help";
    private static final String K_SOS     = "opt_sos";
    private static final String K_MUTED   = "muted";

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MagenCircle"); t.setDaemon(true); return t;
    });

    private CircleClient() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ── local opt-in state ──
    public static boolean optReceive(Context c) { return p(c).getBoolean(K_RECEIVE, false); }
    public static boolean optHelp(Context c)    { return p(c).getBoolean(K_HELP, false); }
    public static boolean optSos(Context c)     { return p(c).getBoolean(K_SOS, false); }
    public static boolean muted(Context c)      { return p(c).getBoolean(K_MUTED, false); }

    /** True if the user participates in the circle at all. When false, no Circle calls are made. */
    public static boolean participating(Context c) {
        return optReceive(c) || optHelp(c) || optSos(c);
    }

    /** Persist opt-in locally and push it to the server. Runs on a background thread. */
    public static void setOptInAsync(Context c, boolean receive, boolean help, boolean sos, boolean muted) {
        Context app = c.getApplicationContext();
        p(app).edit().putBoolean(K_RECEIVE, receive).putBoolean(K_HELP, help)
                .putBoolean(K_SOS, sos).putBoolean(K_MUTED, muted).apply();
        EXEC.execute(() -> {
            if (!ServerConfig.ready(app)) return;
            try {
                JSONObject body = new JSONObject().put("opt_receive", receive).put("opt_help", help)
                        .put("opt_sos", sos).put("muted", muted);
                MagenApiClient.signedPost(app, "/v1/circle/optin", body, false);
            } catch (Exception e) {
                Log.w(TAG, "optin failed: " + e.getMessage());
            }
        });
    }

    // ── blocking calls (run these on a background thread from the UI) ──

    /** Send "it's hard now". Returns the server JSON (request_token, helpers_reached, crisis...). */
    public static JSONObject requestSupportBlocking(Context c, String note) throws Exception {
        JSONObject body = new JSONObject().put("trigger_type", "SOS");
        if (note != null && !note.trim().isEmpty()) body.put("note", note.trim());
        return MagenApiClient.signedPost(c.getApplicationContext(), "/v1/support/request", body, false);
    }

    public static JSONObject feedBlocking(Context c) throws Exception {
        return MagenApiClient.signedGet(c.getApplicationContext(), "/v1/support/feed", false);
    }

    public static JSONObject inboxBlocking(Context c) throws Exception {
        return MagenApiClient.signedGet(c.getApplicationContext(), "/v1/support/inbox", false);
    }

    public static JSONObject sendMessageBlocking(Context c, String requestToken, String text) throws Exception {
        JSONObject body = new JSONObject().put("request_token", requestToken).put("text", text).put("kind", "MESSAGE");
        return MagenApiClient.signedPost(c.getApplicationContext(), "/v1/support/message", body, false);
    }

    public static void reportAsync(Context c, String messageToken) {
        Context app = c.getApplicationContext();
        EXEC.execute(() -> {
            try {
                MagenApiClient.signedPost(app, "/v1/support/report",
                        new JSONObject().put("message_token", messageToken), false);
            } catch (Exception e) { Log.w(TAG, "report failed: " + e.getMessage()); }
        });
    }

    // ── real notifications poll (called from the periodic health reporter) ──

    /**
     * Poll the server for real, deduplicated alerts and raise a local notification per alert.
     * No-op when the user is not participating or the server is not ready. Blocking — called from an
     * already-background scheduler thread.
     */
    public static void pollNotificationsBlocking(Context c) {
        Context app = c.getApplicationContext();
        if (!participating(app) || !ServerConfig.ready(app)) return;
        try {
            JSONObject res = MagenApiClient.signedGet(app, "/v1/support/notifications", false);
            JSONArray alerts = res.optJSONArray("alerts");
            if (alerts == null) return;
            for (int i = 0; i < alerts.length(); i++) {
                JSONObject a = alerts.optJSONObject(i);
                if (a == null) continue;
                String type = a.optString("type", "");
                if ("HELP_NEEDED".equals(type)) {
                    NotificationHelper.notifyCircle(app, "מעגל חיזוק",
                            a.optString("text", "מישהו מהמעגל צריך חיזוק עכשיו"), true);
                } else if ("SUPPORT_RECEIVED".equals(type)) {
                    NotificationHelper.notifyCircle(app, "מעגל חיזוק",
                            a.optString("text", "קיבלת חיזוק מהמעגל"), false);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "notifications poll failed: " + e.getMessage());
        }
    }
}
