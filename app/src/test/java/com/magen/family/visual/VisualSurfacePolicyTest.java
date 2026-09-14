package com.magen.family.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class VisualSurfacePolicyTest {
    @Test public void masksBrowserGoogleAndRegularYoutubeSurfaces() {
        assertTrue(VisualSurfacePolicy.isRegionMaskSurface("com.android.chrome"));
        assertTrue(VisualSurfacePolicy.isRegionMaskSurface("com.google.android.googlequicksearchbox"));
        assertTrue(VisualSurfacePolicy.isRegionMaskSurface("com.google.android.youtube"));
        assertTrue(VisualSurfacePolicy.isRegionMaskSurface("org.mozilla.firefox"));
    }

    @Test public void doesNotRouteTikTokToRegionMask() {
        assertFalse(VisualSurfacePolicy.isRegionMaskSurface("com.zhiliaoapp.musically"));
        assertFalse(VisualSurfacePolicy.isRegionMaskSurface("com.ss.android.ugc.trill"));
    }
}
