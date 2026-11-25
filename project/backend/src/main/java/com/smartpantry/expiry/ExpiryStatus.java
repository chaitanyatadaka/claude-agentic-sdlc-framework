package com.smartpantry.expiry;

/**
 * Expiry classification (REQ-010 to REQ-013). An item with no expiry date is
 * classified as VALID here and has no date bound; the REST layer reports it as
 * "none" (see ItemController).
 */
public enum ExpiryStatus {
    EXPIRED,
    EXPIRING_SOON,
    VALID
}
