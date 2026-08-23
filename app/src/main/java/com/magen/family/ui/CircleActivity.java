package com.magen.family.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.magen.family.server.CircleClient;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * CircleActivity — מעגל חיזוק (אנונימי).
 *
 * מתגי הצטרפות (הכל opt-in, ברירת מחדל כבוי), כפתור "קשה לי עכשיו", זרם החיזוקים
 * שהגיעו אליי, ורשימת מי שצריך חיזוק כדי לעזור. אנונימי לגמרי — אין שמות.
 */
public class CircleActivity extends BaseActivity {

    private LinearLayout inboxBox;
    private LinearLayout feedBox;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);
        setContentView(scroll);

        title(root, "מעגל חיזוק");
        subtitle(root, "אנונימי לגמרי. כשקשה — לא לבד. אתה בוחר אם ואיך להשתתף.");

        // ── מתגי הצטרפות ──
        final Switch swReceive = addSwitch(root, "לקבל חיזוק כשקשה לי", CircleClient.optReceive(this));
        final Switch swHelp    = addSwitch(root, "לחזק אחרים במעגל", CircleClient.optHelp(this));
        final Switch swSos     = addSwitch(root, "לאפשר כפתור \"קשה לי עכשיו\"", CircleClient.optSos(this));
        final Switch swMuted   = addSwitch(root, "השתק חיזוקים נכנסים", CircleClient.muted(this));

        View.OnClickListener saveOpt = v -> CircleClient.setOptInAsync(this,
                swReceive.isChecked(), swHelp.isChecked(), swSos.isChecked(), swMuted.isChecked());
        swReceive.setOnClickListener(saveOpt);
        swHelp.setOnClickListener(saveOpt);
        swSos.setOnClickListener(saveOpt);
        swMuted.setOnClickListener(saveOpt);

        // ── כפתור "קשה לי עכשיו" ──
        Button sos = new Button(this);
        sos.setAllCaps(false);
        sos.setText("💛 קשה לי עכשיו — בקש חיזוק");
        sos.setPadding(0, dp(10), 0, 0);
        root.addView(sos);
        sos.setOnClickListener(v -> requestSupport());

        // ── זרם חיזוקים שהגיעו אליי ──
        title(root, "החיזוקים שהגיעו אליי");
        inboxBox = new LinearLayout(this);
        inboxBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(inboxBox);

        // ── עזור למישהו ──
        title(root, "מישהו צריך חיזוק");
        feedBox = new LinearLayout(this);
        feedBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(feedBox);

        Button refresh = new Button(this);
        refresh.setAllCaps(false);
        refresh.setText("רענן");
        root.addView(refresh);
        refresh.setOnClickListener(v -> reload());
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    // ── actions ──

    private void requestSupport() {
        new Thread(() -> {
            try {
                JSONObject res = CircleClient.requestSupportBlocking(this, null);
                if (res.optBoolean("crisis", false)) {
                    JSONObject help = res.optJSONObject("help");
                    final String msg = help != null ? help.optString("he",
                            "אתה לא לבד. פנה לעזרה מקצועית.") : "אתה לא לבד. פנה לעזרה מקצועית.";
                    ui.post(() -> longToast(msg));
                    return;
                }
                int reached = res.optInt("helpers_reached", 0);
                ui.post(() -> longToast(reached > 0
                        ? "הבקשה נשלחה ל-" + reached + " חברים מהמעגל. תחזיק מעמד 💛"
                        : "הבקשה נרשמה. חברים יראו אותה בקרוב."));
            } catch (Exception e) {
                ui.post(() -> longToast("לא הצלחנו לשלוח כרגע. נסה שוב."));
            }
        }, "CircleSos").start();
    }

    private void reload() {
        if (inboxBox != null) inboxBox.removeAllViews();
        if (feedBox != null) feedBox.removeAllViews();
        new Thread(() -> {
            JSONObject inbox = null, feed = null;
            try { inbox = CircleClient.inboxBlocking(this); } catch (Exception ignored) {}
            try { feed  = CircleClient.feedBlocking(this); } catch (Exception ignored) {}
            final JSONObject fInbox = inbox, fFeed = feed;
            ui.post(() -> {
                renderInbox(fInbox);
                renderFeed(fFeed);
            });
        }, "CircleReload").start();
    }

    private void renderInbox(JSONObject inbox) {
        if (inboxBox == null) return;
        inboxBox.removeAllViews();
        JSONArray items = inbox == null ? null : inbox.optJSONArray("items");
        if (items == null || items.length() == 0) {
            inboxBox.addView(muted("עדיין אין חיזוקים כאן."));
            return;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject m = items.optJSONObject(i);
            if (m == null) continue;
            LinearLayout row = card();
            TextView tv = new TextView(this);
            tv.setText("💬 " + m.optString("text", ""));
            tv.setTextSize(15);
            row.addView(tv);
            final String token = m.optString("message_token", "");
            Button report = new Button(this);
            report.setAllCaps(false);
            report.setText("דווח");
            report.setOnClickListener(v -> {
                CircleClient.reportAsync(this, token);
                shortToast("דווח. תודה.");
                v.setEnabled(false);
            });
            row.addView(report);
            inboxBox.addView(row);
        }
    }

    private void renderFeed(JSONObject feed) {
        if (feedBox == null) return;
        feedBox.removeAllViews();
        JSONArray items = feed == null ? null : feed.optJSONArray("items");
        if (items == null || items.length() == 0) {
            feedBox.addView(muted(CircleClient.optHelp(this)
                    ? "כרגע אף אחד לא צריך חיזוק. תודה שאתה כאן."
                    : "הפעל \"לחזק אחרים\" כדי לראות מי צריך חיזוק."));
            return;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject r = items.optJSONObject(i);
            if (r == null) continue;
            final String token = r.optString("request_token", "");
            LinearLayout row = card();
            TextView tv = new TextView(this);
            tv.setText("🤝 חבר/ה מהמעגל צריך/ה חיזוק");
            tv.setTextSize(15);
            row.addView(tv);
            Button send = new Button(this);
            send.setAllCaps(false);
            send.setText("שלח חיזוק");
            send.setOnClickListener(v -> promptAndSend(token));
            row.addView(send);
            feedBox.addView(row);
        }
    }

    private void promptAndSend(String requestToken) {
        final EditText input = new EditText(this);
        input.setHint("כתוב משפט מחזק... (בלי קישורים/פרטי קשר)");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        new android.app.AlertDialog.Builder(this)
                .setTitle("שלח חיזוק אנונימי")
                .setView(input)
                .setNegativeButton("ביטול", null)
                .setPositiveButton("שלח", (d, w) -> {
                    final String text = input.getText().toString().trim();
                    if (text.isEmpty()) return;
                    new Thread(() -> {
                        try {
                            CircleClient.sendMessageBlocking(this, requestToken, text);
                            ui.post(() -> { shortToast("נשלח 💛"); reload(); });
                        } catch (Exception e) {
                            ui.post(() -> longToast("ההודעה לא עברה (אולי קישור/תוכן לא מתאים) או שהגעת למגבלה. נסה שוב."));
                        }
                    }, "CircleSend").start();
                })
                .show();
    }

    // ── small UI helpers ──

    private Switch addSwitch(LinearLayout parent, String label, boolean checked) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setChecked(checked);
        s.setPadding(0, dp(6), 0, dp(6));
        parent.addView(s);
        return s;
    }

    private void title(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setPadding(0, dp(16), 0, dp(6));
        parent.addView(tv);
    }

    private void subtitle(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0xFF888888);
        parent.addView(tv);
    }

    private TextView muted(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF888888);
        tv.setPadding(0, dp(6), 0, dp(6));
        return tv;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.START);
        int p = dp(10);
        c.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        c.setLayoutParams(lp);
        return c;
    }

    private void shortToast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
    private void longToast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
