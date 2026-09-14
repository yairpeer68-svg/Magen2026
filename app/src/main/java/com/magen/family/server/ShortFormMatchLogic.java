package com.magen.family.server;

import com.magen.family.visual.ShortFormFingerprint;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Pure matching rules kept separate so false-positive protections are unit-testable. */
public final class ShortFormMatchLogic {
    private ShortFormMatchLogic() {}

    public static String reason(ShortFormFingerprint fp, String frame, String center, String text,
                                Set<String> evidence, Set<String> strong) {
        if (fp == null) return null;
        if (evidence == null) evidence = Collections.emptySet();
        if (strong == null) strong = Collections.emptySet();
        if (!fp.textHash.isEmpty() && fp.textHash.equals(text)) return "text_hash";
        if (intersects(fp.strongEvidenceHashes, strong)) return "strong_evidence";
        if (sharedCount(fp.evidenceHashes, evidence) >= 2) return "evidence_2plus";
        if (visualPair(fp.frameHash, fp.centerHash, frame, center)) return "visual_pair";
        return null;
    }

    static boolean visualPair(String aFrame, String aCenter, String bFrame, String bCenter) {
        if (aFrame == null || aCenter == null || bFrame == null || bCenter == null ||
                aFrame.length()!=16 || aCenter.length()!=16 || bFrame.length()!=16 || bCenter.length()!=16) return false;
        try {
            long a=Long.parseUnsignedLong(aFrame,16), b=Long.parseUnsignedLong(bFrame,16);
            long c=Long.parseUnsignedLong(aCenter,16), d=Long.parseUnsignedLong(bCenter,16);
            return Long.bitCount(a^b)<=4 && Long.bitCount(c^d)<=4;
        } catch (Exception ignored) { return false; }
    }

    private static boolean intersects(List<String> a, Set<String> b) {
        if (a == null || b == null || b.isEmpty()) return false;
        for (String x : a) if (b.contains(x)) return true;
        return false;
    }

    private static int sharedCount(List<String> a, Set<String> b) {
        if (a == null || b == null) return 0;
        int n=0; for (String x : a) if (b.contains(x) && ++n>=2) return n; return n;
    }
}
