package com.magen.family.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.magen.family.visual.ShortFormFingerprint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Signed global manual-feedback cache. Raw media/text is never stored here. */
public final class ShortFormVerdictCache {
    private static final String TAG = "MagenShortFormCache";
    private static final String PREFS = "magen_shortform_global";
    private static final String K_SNAPSHOT = "snapshot_json";
    private static final String K_FETCHED = "fetched_at";
    private static final long REFRESH_MS = 60_000L;
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MagenShortFormSync"); t.setDaemon(true); return t;
    });
    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean(false);
    private static volatile boolean loaded;
    private static volatile Map<String,List<Entry>> byPackage = Collections.emptyMap();

    private ShortFormVerdictCache() {}

    public static final class Match {
        public final String fingerprintId;
        public final String reason;
        Match(String id, String reason) { this.fingerprintId=id; this.reason=reason; }
    }

    private static final class Entry {
        String id,pkg,frame,center,text;
        Set<String> evidence=Collections.emptySet();
        Set<String> strong=Collections.emptySet();
    }

    public static void refreshAsync(Context ctx) {
        Context app=ctx.getApplicationContext();
        ensureLoaded(app);
        long last=prefs(app).getLong(K_FETCHED,0L);
        if(System.currentTimeMillis()-last<REFRESH_MS || !ServerConfig.ready(app) || !IN_FLIGHT.compareAndSet(false,true)) return;
        EXEC.execute(() -> { try { refreshBlocking(app); } catch(Exception e){ Log.w(TAG,"snapshot refresh failed: "+e.getMessage()); }
            finally { IN_FLIGHT.set(false); } });
    }

    public static void refreshBlocking(Context ctx) throws Exception {
        Context app=ctx.getApplicationContext();
        if(!ServerConfig.ready(app)) return;
        JSONObject payload=MagenApiClient.signedGet(app,"/v1/shortform/snapshot",true);
        applySnapshot(app,payload,true);
    }

    public static Match match(Context ctx,String pkg,ShortFormFingerprint fp) {
        if(pkg==null||fp==null) return null;
        ensureLoaded(ctx.getApplicationContext());
        List<Entry> entries=byPackage.get(pkg);
        if(entries==null||entries.isEmpty()) return null;
        for(Entry e:entries){
            String reason=ShortFormMatchLogic.reason(fp,e.frame,e.center,e.text,e.evidence,e.strong);
            if(reason!=null) return new Match(e.id,reason);
        }
        return null;
    }

    public static synchronized void addLocal(Context ctx,String pkg,ShortFormFingerprint fp) {
        if(fp==null||!fp.isUsable()) return;
        Context app=ctx.getApplicationContext(); ensureLoaded(app);
        Entry e=new Entry(); e.id=fp.fingerprintId(pkg); e.pkg=pkg; e.frame=fp.frameHash; e.center=fp.centerHash;
        e.text=fp.textHash; e.evidence=new HashSet<>(fp.evidenceHashes); e.strong=new HashSet<>(fp.strongEvidenceHashes);
        Map<String,List<Entry>> copy=new HashMap<>(byPackage);
        List<Entry> list=new ArrayList<>(copy.getOrDefault(pkg,Collections.emptyList()));
        boolean exists=false; for(Entry x:list) if(e.id.equals(x.id)){exists=true;break;}
        if(!exists){ list.add(0,e); if(list.size()>2100) list=list.subList(0,2100); }
        copy.put(pkg,Collections.unmodifiableList(new ArrayList<>(list)));
        byPackage=Collections.unmodifiableMap(copy);
    }

    private static synchronized void ensureLoaded(Context ctx){
        if(loaded)return; loaded=true;
        String raw=prefs(ctx).getString(K_SNAPSHOT,"");
        if(raw==null||raw.isEmpty()){byPackage=Collections.emptyMap();return;}
        try{applySnapshot(ctx,new JSONObject(raw),false);}catch(Exception e){byPackage=Collections.emptyMap();}
    }

    private static synchronized void applySnapshot(Context ctx,JSONObject payload,boolean persist)throws Exception{
        JSONArray items=payload.optJSONArray("items"); Map<String,List<Entry>> map=new HashMap<>();
        if(items!=null)for(int i=0;i<items.length();i++){
            JSONObject o=items.optJSONObject(i); if(o==null)continue;
            Entry e=new Entry(); e.id=o.optString("fingerprint_id",""); e.pkg=o.optString("package_name","");
            e.frame=o.optString("frame_hash",""); e.center=o.optString("center_hash",""); e.text=o.optString("text_hash","");
            e.evidence=jsonSet(o.optJSONArray("evidence_hashes"),12); e.strong=jsonSet(o.optJSONArray("strong_evidence_hashes"),8);
            if(e.id.isEmpty()||e.pkg.isEmpty())continue;
            map.computeIfAbsent(e.pkg,k->new ArrayList<>()).add(e);
        }
        Map<String,List<Entry>> frozen=new HashMap<>(); for(Map.Entry<String,List<Entry>> me:map.entrySet()) frozen.put(me.getKey(),Collections.unmodifiableList(me.getValue()));
        byPackage=Collections.unmodifiableMap(frozen);
        if(persist)prefs(ctx).edit().putString(K_SNAPSHOT,payload.toString()).putLong(K_FETCHED,System.currentTimeMillis()).apply();
    }

    private static Set<String> jsonSet(JSONArray a,int max){ HashSet<String>s=new HashSet<>(); if(a==null)return s; for(int i=0;i<a.length()&&s.size()<max;i++){String v=a.optString(i,"");if(!v.isEmpty())s.add(v);} return s; }
    private static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
}
