package com.magen.family.filter;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Guards the browser-coverage hardening: the URL/keyword filter only reads a browser's address bar
 * for packages it recognizes. These assertions fail if a known browser is accidentally removed from
 * {@link ContentFilter#BROWSER_PACKAGES}.
 */
public class BrowserCoverageTest {

    @Test public void coversMainstreamAndForkBrowsers() {
        String[] expected = {
            "com.android.chrome", "org.mozilla.firefox", "com.microsoft.emmx",
            "com.brave.browser", "com.opera.browser", "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android",
            // hardening additions
            "com.vivaldi.browser", "com.yandex.browser", "com.mi.globalbrowser",
            "com.huawei.browser", "org.mozilla.focus", "com.ecosia.android",
        };
        for (String pkg : expected) {
            assertTrue("missing browser coverage: " + pkg,
                ContentFilter.BROWSER_PACKAGES.contains(pkg));
        }
    }
}
