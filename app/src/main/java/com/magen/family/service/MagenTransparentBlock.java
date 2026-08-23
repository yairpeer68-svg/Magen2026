package com.magen.family.service;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * חסימה שקופה — בולעת לחיצות ומונעת אינטראקציה
 * עדין יותר מ-HOME_ACTION, בדיוק כמו רימון
 */
public class MagenTransparentBlock {

    private static WindowManager windowManager;
    private static FrameLayout blockView;
    private static WindowManager.LayoutParams layoutParams;
    private static boolean isShowing = false;
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    // טיימר בטחון — מסיר את החסימה השקופה כמעט מיד.
    //
    // ⚠️ תיקון באג "הטלפון קופא לחמש שניות": ה-overlay הזה פרוש על כל המסך
    // ובולע *כל* מגע (onTouch מחזיר true). הוא נועד רק לבלוע נקישות בזמן
    // מעבר ה-HOME אחרי חסימה — עניין של פחות מחצי שנייה. הערך הקודם (5000ms)
    // גרם לכך שאחרי כל חסימת תוכן המשתמש לא יכול היה לגעת בכלום במשך חמש
    // שניות (וגם בלע את הנקישה על "אישור" בהתקנת CA). 600ms מספיק בהחלט
    // כדי לכסות את מעבר ה-HOME/BACK בלי לנעול את הטלפון.
    private static final long BLOCK_MS = 600L;
    private static final Runnable safetyWatchdog = MagenTransparentBlock::hide;

    public static void show(Context context) {
        uiHandler.post(() -> {
            try {
                if (isShowing) {
                    uiHandler.removeCallbacks(safetyWatchdog);
                    uiHandler.postDelayed(safetyWatchdog, BLOCK_MS);
                    return;
                }

                windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

                blockView = new FrameLayout(context);
                blockView.setBackgroundColor(Color.parseColor("#01000000")); // שקוף כמעט

                // בלע לחיצות — Touch Hijacking
                blockView.setOnTouchListener((v, event) -> true);

                int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

                layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutParams.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                }

                windowManager.addView(blockView, layoutParams);
                isShowing = true;

                uiHandler.removeCallbacks(safetyWatchdog);
                uiHandler.postDelayed(safetyWatchdog, BLOCK_MS);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void hide() {
        uiHandler.post(() -> {
            try {
                uiHandler.removeCallbacks(safetyWatchdog);
                if (windowManager != null && blockView != null && isShowing) {
                    windowManager.removeView(blockView);
                    isShowing = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static boolean isActive() {
        return isShowing;
    }
}
