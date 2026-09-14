package com.magen.family.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortFormFingerprintTest {
    @Test public void changingCountersDoesNotChangeTextFingerprint() {
        ShortFormFingerprint a=ShortFormFingerprint.textOnly("@creator | same caption here | 62.3K | 222");
        ShortFormFingerprint b=ShortFormFingerprint.textOnly("@creator | same caption here | 71.8K | 999");
        assertFalse(a.textHash.isEmpty());
        assertEquals(a.textHash,b.textHash);
        assertEquals(a.evidenceHashes,b.evidenceHashes);
    }

    @Test public void meaningfulCaptionChangeChangesFingerprint() {
        ShortFormFingerprint a=ShortFormFingerprint.textOnly("@creator | first unique caption text");
        ShortFormFingerprint b=ShortFormFingerprint.textOnly("@creator | completely different caption text");
        assertNotEquals(a.textHash,b.textHash);
    }

    @Test public void fingerprintIdIsStableAndPrivacySafeShape() {
        ShortFormFingerprint f=ShortFormFingerprint.textOnly("@creator | stable unique caption text");
        String id1=f.fingerprintId("com.zhiliaoapp.musically");
        String id2=f.fingerprintId("com.zhiliaoapp.musically");
        assertEquals(id1,id2);
        assertTrue(id1.matches("[0-9a-f]{64}"));
    }
}
