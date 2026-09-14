package com.magen.family.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.magen.family.visual.ShortFormFingerprint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Persistent manual-report queue. Auto-skips never call this class. */
public final class ShortFormFeedbackClient {
    private static final String TAG="MagenShortFormReport";
    private static final String PREFS="magen_shortform_reports";
    private static final String K_PENDING="pending";
    private static final int MAX_PENDING=64;
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"MagenShortFormReport");t.setDaemon(true);return t;});
    private ShortFormFeedbackClient(){}

    public interface Callback { void onDone(boolean uploaded); }

    public static void reportAsync(Context ctx,String pkg,ShortFormFingerprint fp,Callback cb){
        Context app=ctx.getApplicationContext();
        try{
            JSONObject body=fp.toJson(pkg);
            enqueue(app,body);
            ShortFormVerdictCache.addLocal(app,pkg,fp);
        }catch(Exception e){ if(cb!=null)cb.onDone(false); return; }
        EXEC.execute(()->{ boolean ok=false; try{ ok=flushPendingBlocking(app)>0; }catch(Exception e){Log.w(TAG,"report upload failed: "+e.getMessage());}
            if(cb!=null){ final boolean result=ok; new android.os.Handler(android.os.Looper.getMainLooper()).post(()->cb.onDone(result)); } });
    }

    public static void flushPendingAsync(Context ctx){Context app=ctx.getApplicationContext();EXEC.execute(()->{try{flushPendingBlocking(app);}catch(Exception e){Log.w(TAG,"flush failed: "+e.getMessage());}});}

    public static int flushPendingBlocking(Context ctx)throws Exception{
        Context app=ctx.getApplicationContext(); if(!ServerConfig.ready(app))return 0;
        List<JSONObject> pending=load(app); int sent=0;
        for(JSONObject body:new ArrayList<>(pending)){
            try{ MagenApiClient.signedPost(app,"/v1/shortform/report",body,true); remove(app,body); sent++; }
            catch(Exception e){ if(sent==0)throw e; break; }
        }
        if(sent>0)try{ShortFormVerdictCache.refreshBlocking(app);}catch(Exception ignored){}
        return sent;
    }

    private static synchronized void enqueue(Context c,JSONObject body){
        List<JSONObject> list=load(c); String key=body.optString("package_name","")+"|"+body.optString("text_hash","")+"|"+body.optString("frame_hash","");
        for(JSONObject o:list){String k=o.optString("package_name","")+"|"+o.optString("text_hash","")+"|"+o.optString("frame_hash","");if(key.equals(k))return;}
        list.add(body); while(list.size()>MAX_PENDING)list.remove(0); save(c,list);
    }
    private static synchronized void remove(Context c,JSONObject body){List<JSONObject>list=load(c);String raw=body.toString();for(int i=0;i<list.size();i++)if(raw.equals(list.get(i).toString())){list.remove(i);break;}save(c,list);}
    private static List<JSONObject> load(Context c){ArrayList<JSONObject>out=new ArrayList<>();String raw=p(c).getString(K_PENDING,"[]");try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)out.add(o);}}catch(Exception ignored){}return out;}
    private static void save(Context c,List<JSONObject>list){JSONArray a=new JSONArray();for(JSONObject o:list)a.put(o);p(c).edit().putString(K_PENDING,a.toString()).apply();}
    private static SharedPreferences p(Context c){return c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
}
