package com.smartpantry.expiry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ExpiryClassifierTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    private final ExpiryClassifier classifier = new ExpiryClassifier(fixedClock(TODAY));

    static Clock fixedClock(LocalDate day) {
        ZoneId zone = ZoneId.systemDefault();
        return Clock.fixed(day.atStartOfDay(zone).toInstant(), zone);
    }

    @Test
    void expiryYesterdayIsExpired() {
        assertEquals(ExpiryStatus.EXPIRED, classifier.classify(TODAY.minusDays(1)));
    }

    @Test
    void expiryTodayIsExpiringSoon() {
        assertEquals(ExpiryStatus.EXPIRING_SOON, classifier.classify(TODAY));
    }

    @Test
    void expiryInThreeDaysIsExpiringSoon() {
        assertEquals(ExpiryStatus.EXPIRING_SOON, classifier.classify(TODAY.plusDays(3)));
    }

    @Test
    void expiryInFourDaysIsValid() {
        assertEquals(ExpiryStatus.VALID, classifier.classify(TODAY.plusDays(4)));
    }

    @Test
    void nullExpiryIsValidAndNeverExpired() {
        assertEquals(ExpiryStatus.VALID, classifier.classify(null));
    }

    @Test
    void boundsFollowTheInjectedClockNotTheSystemDate() {
        ExpiryClassifier nextDay = new ExpiryClassifier(fixedClock(TODAY.plusDays(1)));
        assertEquals(ExpiryStatus.EXPIRED, nextDay.classify(TODAY));
    }
}
