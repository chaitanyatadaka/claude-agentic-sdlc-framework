package com.smartpantry.expiry;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

/**
 * Shared expiry classifier (NFR-011). Bounds, with "today" from the injected Clock:
 * EXPIRED when the expiry date is before today; EXPIRING_SOON from today through
 * today plus 3 days inclusive; VALID from today plus 4 days on. A null expiry
 * date gives VALID and has no bound.
 */
@Component
public class ExpiryClassifier {

    static final int EXPIRING_WINDOW_DAYS = 3;

    private final Clock clock;

    public ExpiryClassifier(Clock clock) {
        this.clock = clock;
    }

    public ExpiryStatus classify(LocalDate expiryDate) {
        if (expiryDate == null) {
            return ExpiryStatus.VALID;
        }
        LocalDate today = LocalDate.now(clock);
        if (expiryDate.isBefore(today)) {
            return ExpiryStatus.EXPIRED;
        }
        if (!expiryDate.isAfter(today.plusDays(EXPIRING_WINDOW_DAYS))) {
            return ExpiryStatus.EXPIRING_SOON;
        }
        return ExpiryStatus.VALID;
    }
}
