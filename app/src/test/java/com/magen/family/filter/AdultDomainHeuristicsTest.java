package com.magen.family.filter;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdultDomainHeuristicsTest {
    @Test public void catchesObservedAndMirrorDomains() {
        assertTrue(AdultDomainHeuristics.isClearlyAdultHost("fakfap.com"));
        assertTrue(AdultDomainHeuristics.isClearlyAdultHost("cdn.myfap.example"));
        assertTrue(AdultDomainHeuristics.isClearlyAdultHost("new-porn-mirror.example"));
        assertTrue(AdultDomainHeuristics.isClearlyAdultHost("xvideos-proxy.example"));
    }

    @Test public void avoidsKnownFapPrefixFalsePositiveShape() {
        assertFalse(AdultDomainHeuristics.isClearlyAdultHost("fapiao.com"));
        assertFalse(AdultDomainHeuristics.isClearlyAdultHost("example.com"));
        assertFalse(AdultDomainHeuristics.isClearlyAdultHost("wikipedia.org"));
    }
}
