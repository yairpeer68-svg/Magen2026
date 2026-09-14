package com.magen.family.visual;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Local accessibility overlay that masks only the risky visual tile. */
public final class MagenVisualRegionMask {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long SAFETY_HIDE_MS = 2600L;
    private static WindowManager wm;
    private static FrameLayout view;
    private static final Runnable SAFETY_HIDE = MagenVisualRegionMask::hideNow;

    private MagenVisualRegionMask() {}

    public static void show(AccessibilityService service, int tileIndex, String label) {
        if (service == null) return;
        MAIN.post(() -> showNow(service, tileIndex));
    }

    public static void hide() { MAIN.post(MagenVisualRegionMask::hideNow); }

    private static void showNow(AccessibilityService service, int tileIndex) {
        try {
            hideNow();
            int width = Math.max(1, service.getResources().getDisplayMetrics().widthPixels);
            int height = Math.max(1, service.getResources().getDisplayMetrics().heightPixels);

            int x, y, w, h;
            if (tileIndex >= 1 && tileIndex <= 6) {
                int zero = tileIndex - 1;
                int row = zero / 3, col = zero % 3;
                int top = Math.round(height * 0.08f);
                int bottom = Math.round(height * 0.94f);
                int contentH = Math.max(1, bottom - top);
                x = col * width / 3;
                int x1 = (col + 1) * width / 3;
                y = top + row * contentH / 2;
                int y1 = top + (row + 1) * contentH / 2;
                w = Math.max(1, x1 - x); h = Math.max(1, y1 - y);
            } else {
                // Full-screen classifier could not localize. Mask the content viewport, never HOME/BACK.
                x = 0; y = Math.round(height * 0.08f); w = width; h = Math.round(height * 0.86f);
            }

            wm = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
            view = new FrameLayout(service);
            view.setBackgroundColor(Color.rgb(34, 38, 46));

            TextView msg = new TextView(service);
            msg.setText("🛡️  תוכן מסונן");
            msg.setTextColor(Color.WHITE);
            msg.setTextSize(16);
            msg.setGravity(Gravity.CENTER);
            view.addView(msg, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                w, h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE);
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.x = x; lp.y = y;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            wm.addView(view, lp);
            MAIN.removeCallbacks(SAFETY_HIDE);
            MAIN.postDelayed(SAFETY_HIDE, SAFETY_HIDE_MS);
        } catch (Exception ignored) { hideNow(); }
    }

    private static void hideNow() {
        MAIN.removeCallbacks(SAFETY_HIDE);
        try { if (wm != null && view != null) wm.removeView(view); } catch (Exception ignored) {}
        view = null; wm = null;
    }
}
