package com.magen.family.filter;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExplicitSearchQueryTest {
    private AhoCorasick matcher() {
        AhoCorasick m = new AhoCorasick();
        m.addPattern("porn"); m.addPattern("sex"); m.addPattern("pornhub"); m.build();
        return m;
    }

    @Test public void blocksDirectAndPunctuationEvasion() {
        AhoCorasick m = matcher();
        assertTrue(ExplicitSearchQuery.shouldBlock("porn", m, true));
        assertTrue(ExplicitSearchQuery.shouldBlock("por,n", m, true));
        assertTrue(ExplicitSearchQuery.shouldBlock("por n", m, true));
        assertTrue(ExplicitSearchQuery.shouldBlock("p0rn", m, true));
        assertTrue(ExplicitSearchQuery.shouldBlock("free pornhub videos", m, true));
    }

    @Test public void highConfidenceStillBlocksInLightMode() {
        assertTrue(ExplicitSearchQuery.shouldBlock("por,n", matcher(), false));
        assertTrue(ExplicitSearchQuery.shouldBlock("xvideos", matcher(), false));
    }

    @Test public void doesNotBlockBenignLookalikes() {
        AhoCorasick m = matcher();
        assertFalse(ExplicitSearchQuery.shouldBlock("Essex university", m, true));
        assertFalse(ExplicitSearchQuery.shouldBlock("document analysis", m, true));
        assertFalse(ExplicitSearchQuery.shouldBlock("sports news", m, true));
    }
}
