package com.magen.family.mitm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MitmPolicyTest {
    @Test
    public void sensitiveProvidersAreConservativelyBypassed() {
        assertTrue(MitmPolicy.shouldBypass("accounts.example.com"));
        assertTrue(MitmPolicy.shouldBypass("secure-onlinebank.example"));
        assertTrue(MitmPolicy.shouldBypass("patient.portal.example"));
        assertTrue(MitmPolicy.shouldBypass("my-passwordvault.example"));
        assertTrue(MitmPolicy.shouldBypass("checkout.shop.example"));
        assertFalse(MitmPolicy.shouldBypass("news.example.com"));
    }
}
