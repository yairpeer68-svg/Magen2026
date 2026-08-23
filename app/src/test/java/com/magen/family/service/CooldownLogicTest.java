package com.magen.family.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure timing logic of the self-control cooldown (Context-free).
 * The prefs-bound wrappers in {@link MagenCooldown} delegate to these.
 */
public class CooldownLogicTest {

    @Test public void noTargetMeansNothingPendingOrElapsed() {
        assertEquals(0L, MagenCooldown.remaining(0L, 5_000L));
        assertFalse(MagenCooldown.elapsed(0L, 5_000L));
        assertFalse(MagenCooldown.pending(0L, 5_000L));
    }

    @Test public void pendingBeforeTargetElapsedAfter() {
        long target = 10_000L;
        // before target → pending, not elapsed, positive remaining
        assertTrue(MagenCooldown.pending(target, 4_000L));
        assertFalse(MagenCooldown.elapsed(target, 4_000L));
        assertEquals(6_000L, MagenCooldown.remaining(target, 4_000L));
        // exactly at / after target → elapsed, not pending, zero remaining
        assertTrue(MagenCooldown.elapsed(target, 10_000L));
        assertFalse(MagenCooldown.pending(target, 10_000L));
        assertEquals(0L, MagenCooldown.remaining(target, 12_000L));
    }

    @Test public void minutesAreClampedToSaneRange() {
        assertEquals(0, MagenCooldown.clampMinutes(-5));
        assertEquals(720, MagenCooldown.clampMinutes(720));
        assertEquals(7 * 24 * 60, MagenCooldown.clampMinutes(999_999));
    }

    @Test public void formatRemainingIsHhMmSs() {
        assertEquals("00:00:00", MagenCooldown.formatRemaining(0L));
        assertEquals("00:00:03", MagenCooldown.formatRemaining(3_000L));
        assertEquals("11:59:59", MagenCooldown.formatRemaining((11L * 3600 + 59 * 60 + 59) * 1000L));
        assertEquals("12:00:00", MagenCooldown.formatRemaining(12L * 3600 * 1000L));
    }

    @Test public void defaultIsTwelveHours() {
        assertEquals(12 * 60, MagenCooldown.DEFAULT_MINUTES);
    }
}
