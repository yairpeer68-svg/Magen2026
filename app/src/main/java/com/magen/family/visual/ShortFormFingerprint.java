package com.magen.family.visual;

import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Privacy-minimized fingerprint for a short-form item. No raw image/text leaves the device. */
public final class ShortFormFingerprint {
    public final String frameHash;
    public final String centerHash;
    public final String textHash;
    public final List<String> evidenceHashes;
    public final List<String> strongEvidenceHashes;

    public ShortFormFingerprint(String frameHash, String centerHash, String textHash,
                                List<String> evidenceHashes, List<String> strongEvidenceHashes) {
        this.frameHash = cleanHash(frameHash, 16);
        this.centerHash = cleanHash(centerHash, 16);
        this.textHash = cleanHash(textHash, 64);
        this.evidenceHashes = immutableUnique(evidenceHashes, 12);
        this.strongEvidenceHashes = immutableUnique(strongEvidenceHashes, 8);
    }

    public static ShortFormFingerprint from(Bitmap screen, String visibleText) {
        String frame = "", center = "";
        if (screen != null && !screen.isRecycled() && screen.getWidth() > 20 && screen.getHeight() > 40) {
            Bitmap content = null, middle = null;
            try {
                int w = screen.getWidth(), h = screen.getHeight();
                int top = Math.max(0, Math.round(h * 0.06f));
                int bottom = Math.min(h, Math.round(h * 0.90f));
                content = Bitmap.createBitmap(screen, 0, top, w, Math.max(1, bottom - top));
                frame = hex64(VisualFrameFingerprint.dHash(content));

                int x0 = Math.max(0, Math.round(w * 0.10f));
                int x1 = Math.min(w, Math.round(w * 0.90f));
                int y0 = Math.max(top, Math.round(h * 0.16f));
                int y1 = Math.min(bottom, Math.round(h * 0.78f));
                middle = Bitmap.createBitmap(screen, x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
                center = hex64(VisualFrameFingerprint.dHash(middle));
            } finally {
                if (content != null && content != screen && !content.isRecycled()) content.recycle();
                if (middle != null && middle != screen && !middle.isRecycled()) middle.recycle();
            }
        }

        TextParts parts = textParts(visibleText);
        return new ShortFormFingerprint(frame, center, parts.fullHash, parts.evidence, parts.strong);
    }

    public static ShortFormFingerprint textOnly(String visibleText) { return from(null, visibleText); }

    public boolean isUsable() {
        return !textHash.isEmpty() || !evidenceHashes.isEmpty() || (!frameHash.isEmpty() && !centerHash.isEmpty());
    }

    public String fingerprintId(String packageName) {
        ArrayList<String> sorted = new ArrayList<>(evidenceHashes);
        Collections.sort(sorted);
        return sha256Hex((safe(packageName) + "|" + frameHash + "|" + centerHash + "|" + textHash + "|" +
            join(sorted)).getBytes(StandardCharsets.UTF_8), 32);
    }

    public JSONObject toJson(String packageName) throws Exception {
        JSONObject o = new JSONObject();
        o.put("package_name", safe(packageName));
        o.put("frame_hash", frameHash);
        o.put("center_hash", centerHash);
        o.put("text_hash", textHash);
        o.put("evidence_hashes", new JSONArray(evidenceHashes));
        o.put("strong_evidence_hashes", new JSONArray(strongEvidenceHashes));
        return o;
    }

    public static ShortFormFingerprint fromJson(JSONObject o) {
        return new ShortFormFingerprint(o.optString("frame_hash", ""), o.optString("center_hash", ""),
            o.optString("text_hash", ""), jsonStrings(o.optJSONArray("evidence_hashes"), 12),
            jsonStrings(o.optJSONArray("strong_evidence_hashes"), 8));
    }

    private static final class TextParts {
        String fullHash = "";
        List<String> evidence = new ArrayList<>();
        List<String> strong = new ArrayList<>();
    }

    private static TextParts textParts(String raw) {
        TextParts out = new TextParts();
        if (raw == null || raw.trim().isEmpty()) return out;
        String[] chunks = raw.split("\\s*\\|\\s*");
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        LinkedHashSet<String> evidence = new LinkedHashSet<>();
        LinkedHashSet<String> strong = new LinkedHashSet<>();
        for (String chunk : chunks) {
            String n = normalizeSegment(chunk);
            if (n.length() < 4 || normalized.contains(n)) continue;
            normalized.add(n);
            String h = sha256Hex(n.getBytes(StandardCharsets.UTF_8), 8);
            evidence.add(h);
            if (n.length() >= 18 && countLetters(n) >= 10) strong.add(h);
            if (normalized.size() >= 12) break;
        }
        if (!normalized.isEmpty()) {
            String joined = join(normalized, "|");
            out.fullHash = sha256Hex(joined.getBytes(StandardCharsets.UTF_8), 16);
        }
        out.evidence = new ArrayList<>(evidence);
        out.strong = new ArrayList<>(strong);
        return out;
    }

    static String normalizeSegment(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        s = s.replaceAll("https?://\\S+", " ");
        s = s.replaceAll("\\p{Nd}+", " ");
        s = s.replaceAll("[^\\p{L}\\p{M}_@#]+", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() > 180 ? s.substring(0, 180).trim() : s;
    }

    private static int countLetters(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) if (Character.isLetter(s.charAt(i))) count++;
        return count;
    }

    private static String hex64(long v) { return String.format(Locale.US, "%016x", v); }

    private static String sha256Hex(byte[] input, int bytes) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(input);
            StringBuilder b = new StringBuilder(bytes * 2);
            for (int i = 0; i < Math.min(bytes, d.length); i++) b.append(String.format(Locale.US, "%02x", d[i] & 0xff));
            return b.toString();
        } catch (Exception e) { return ""; }
    }

    private static String cleanHash(String s, int max) {
        if (s == null) return "";
        String h = s.trim().toLowerCase(Locale.ROOT);
        if (!h.matches("[0-9a-f]+") || h.length() > max) return "";
        return h;
    }

    private static List<String> immutableUnique(List<String> in, int max) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (in != null) for (String s : in) {
            String h = cleanHash(s, 64);
            if (h.length() >= 16) set.add(h);
            if (set.size() >= max) break;
        }
        return Collections.unmodifiableList(new ArrayList<>(set));
    }

    private static List<String> jsonStrings(JSONArray a, int max) {
        ArrayList<String> out = new ArrayList<>();
        if (a == null) return out;
        for (int i = 0; i < a.length() && out.size() < max; i++) {
            String v = a.optString(i, "");
            if (!v.isEmpty()) out.add(v);
        }
        return out;
    }


    private static String join(List<String> values) { return join(values, ","); }
    private static String join(Iterable<String> values, String sep) {
        StringBuilder b=new StringBuilder();
        for(String v:values){ if(b.length()>0)b.append(sep); b.append(v); }
        return b.toString();
    }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
