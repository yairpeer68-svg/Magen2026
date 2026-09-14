package com.magen.family.service.vpn;

import org.junit.Test;
import static org.junit.Assert.*;

public class VpnPolicyResolverTest {
    @Test public void cloudflareFamilyIsProductionDefault() {
        assertEquals("1.1.1.3", VpnPolicy.DEFAULT_UPSTREAM_DNS);
        assertEquals("1.0.0.3", VpnPolicy.FALLBACK_UPSTREAM_DNS);
        assertEquals("1.1.1.3", VpnPolicy.FAMILY_RESOLVERS[0]);
        assertEquals("1.0.0.3", VpnPolicy.FAMILY_RESOLVERS[1]);
    }
}
