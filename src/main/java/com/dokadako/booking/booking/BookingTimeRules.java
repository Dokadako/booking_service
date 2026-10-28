package com.dokadako.booking.booking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.dokadako.booking.common.BadRequestException;

/**
 * Business rules for the time range of a booking, kept apart from the service so they can be
 * unit tested with a fixed clock.
 */
@Component
class BookingTimeRules {

    static final Duration SLOT = Duration.ofMinutes(15);
    static final Duration MIN_DURATION = SLOT;
    static final Duration MAX_DURATION = Duration.ofHours(8);
    static final Duration MAX_ADVANCE = Duration.ofDays(90);

    private final Clock clock;

    BookingTimeRules(Clock clock) {
        this.clock = clock;
    }

    void check(Instant startsAt, Instant endsAt) {
        if (!startsAt.isBefore(endsAt)) {
            throw new BadRequestException("Booking must start before it ends");
        }
        Instant now = Instant.now(clock);
        if (!startsAt.isAfter(now)) {
            throw new BadRequestException("Booking must start in the future");
        }
        if (startsAt.isAfter(now.plus(MAX_ADVANCE))) {
            throw new BadRequestException("Booking can start at most 90 days ahead");
        }
        if (!isAlignedToSlot(startsAt) || !isAlignedToSlot(endsAt)) {
            throw new BadRequestException("Booking must start and end on a 15-minute boundary");
        }
        Duration duration = Duration.between(startsAt, endsAt);
        if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0) {
            throw new BadRequestException("Booking must last from 15 minutes to 8 hours");
        }
    }

    private static boolean isAlignedToSlot(Instant instant) {
        return instant.getNano() == 0 && instant.getEpochSecond() % SLOT.toSeconds() == 0;
    }
}
