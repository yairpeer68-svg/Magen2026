package com.magen.family.visual;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

/** Small touchable accessibility overlay shown only on short-form feeds. */
public final class ShortFormReportOverlay {
    private final AccessibilityService service;
    private final Runnable onReport;
    private final Handler main=new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private TextView view;
    private boolean showing;

    public ShortFormReportOverlay(AccessibilityService service,Runnable onReport){this.service=service;this.onReport=onReport;}
    public boolean isShowing(){return showing;}

    public void show(){main.post(()->{
        if(showing)return;
        try{
            wm=(WindowManager)service.getSystemService(AccessibilityService.WINDOW_SERVICE);
            TextView v=new TextView(service); v.setText("🚩 דווח"); v.setTextColor(Color.WHITE); v.setTextSize(13); v.setGravity(Gravity.CENTER);
            int h=dp(36); v.setPadding(dp(11),0,dp(11),0);
            GradientDrawable bg=new GradientDrawable(); bg.setColor(Color.argb(220,120,24,34)); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1),Color.argb(230,255,130,140)); v.setBackground(bg);
            v.setOnClickListener(x->{ if(onReport!=null)onReport.run(); });
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            lp.gravity=Gravity.TOP|Gravity.END; lp.x=dp(10); lp.y=dp(88);
            wm.addView(v,lp); view=v; showing=true;
        }catch(Exception ignored){showing=false;view=null;}
    });}

    public void hide(){main.post(()->{try{if(showing&&wm!=null&&view!=null)wm.removeView(view);}catch(Exception ignored){}showing=false;view=null;wm=null;});}
    public void close(){hide();}
    private int dp(int v){return Math.round(v*service.getResources().getDisplayMetrics().density);}
}
