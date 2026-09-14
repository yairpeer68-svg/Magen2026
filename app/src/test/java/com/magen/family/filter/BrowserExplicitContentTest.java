package com.magen.family.filter;

import org.junit.Test;
import static org.junit.Assert.*;

public class BrowserExplicitContentTest {
    @Test public void blocksHighConfidenceAdultPageText() {
        assertTrue(BrowserExplicitContent.isClearlyExplicit(
            "FAKFAP CLIP SEX HD, XEM PHIM SEX RO - latest videos"));
        assertTrue(BrowserExplicitContent.isClearlyExplicit(
            "Watch pornography videos here"));
        assertTrue(BrowserExplicitContent.isClearlyExplicit(
            "XNXX free videos"));
    }

    @Test public void singleAmbiguousHealthMentionDoesNotBlock() {
        assertFalse(BrowserExplicitContent.isClearlyExplicit("Sex education and health information"));
        assertFalse(BrowserExplicitContent.isClearlyExplicit("A nude shade of lipstick"));
        assertFalse(BrowserExplicitContent.isClearlyExplicit("Wikipedia encyclopedia"));
    }
}
