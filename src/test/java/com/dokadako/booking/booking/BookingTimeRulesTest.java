package com.dokadako.booking.booking;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.dokadako.booking.common.BadRequestException;

class BookingTimeRulesTest {

    /**
     * On a 15-minute boundary, so a start equal to "now" breaks only the "in the future" rule.
     * Far from the real date, so a rule that read the system clock instead of the injected one would fail.
     */
    private static final Instant NOW = Instant.parse("2030-06-03T10:00:00Z");

    private static final String START_BEFORE_END = "Booking must start before it ends";
    private static final String START_IN_FUTURE = "Booking must start in the future";
    private static final String ALIGNED_TO_SLOT = "Booking must start and end on a 15-minute boundary";
    private static final String DURATION_RANGE = "Booking must last from 15 minutes to 8 hours";

    private final BookingTimeRules rules = rulesAt(NOW);

    // --- valid ranges ---

    @Test
    void shouldAcceptOneHourBookingStartingInTheFuture() {
        // given
        Instant startsAt = NOW.plus(Duration.ofHours(1));
        Instant endsAt = startsAt.plus(Duration.ofHours(1));

        // when / then
        assertThatCode(() -> rules.check(startsAt, endsAt)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "starts at {0}")
    @ValueSource(strings = {
            "2030-06-03T11:00:00Z",
            "2030-06-03T11:15:00Z",
            "2030-06-03T11:30:00Z",
            "2030-06-03T11:45:00Z"})
    void shouldAcceptStartOnEveryQuarterHour(String start) {
        // given
        Instant startsAt = Instant.parse(start);
        Instant endsAt = startsAt.plus(Duration.ofMinutes(30));

        // when / then
        assertThatCode(() -> rules.check(startsAt, endsAt)).doesNotThrowAnyException();
    }

    // --- start before end ---

    @Test
    void shouldRejectEndEqualToStart() {
        // given
        Instant startsAt = NOW.plus(Duration.ofHours(1));

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, startsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(START_BEFORE_END);
    }

    @Test
    void shouldRejectEndBeforeStart() {
        // given
        Instant startsAt = NOW.plus(Duration.ofHours(2));
        Instant endsAt = NOW.plus(Duration.ofHours(1));

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(START_BEFORE_END);
    }

    // --- start strictly in the future ---

    @Test
    void shouldRejectStartEqualToNow() {
        // given
        Instant endsAt = NOW.plus(Duration.ofHours(1));

        // when / then
        assertThatThrownBy(() -> rules.check(NOW, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(START_IN_FUTURE);
    }

    @Test
    void shouldRejectStartInThePast() {
        // given
        Instant startsAt = NOW.minus(Duration.ofDays(1));
        Instant endsAt = startsAt.plus(Duration.ofHours(1));

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(START_IN_FUTURE);
    }

    @Test
    void shouldRejectBookingThatAlreadyStartedAndEndsInTheFuture() {
        // given
        Instant startsAt = NOW.minus(Duration.ofMinutes(15));
        Instant endsAt = NOW.plus(Duration.ofMinutes(45));

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(START_IN_FUTURE);
    }

    @Test
    void shouldRejectStartOneNanosecondBeforeNow() {
        // given
        Instant startsAt = Instant.parse("2030-06-03T10:15:00Z");
        BookingTimeRules rulesJustAfterStart = rulesAt(startsAt.plusNanos(1));

        // when / then
        assertThatThrownBy(() -> rulesJustAfterStart.check(startsAt, startsAt.plus(Duration.ofHours(1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(START_IN_FUTURE);
    }

    @Test
    void shouldAcceptStartOneNanosecondAfterNow() {
        // given
        Instant startsAt = Instant.parse("2030-06-03T10:15:00Z");
        BookingTimeRules rulesJustBeforeStart = rulesAt(startsAt.minusNanos(1));

        // when / then
        assertThatCode(() -> rulesJustBeforeStart.check(startsAt, startsAt.plus(Duration.ofHours(1))))
                .doesNotThrowAnyException();
    }

    // --- 15-minute boundaries ---

    @Test
    void shouldRejectStartNotAlignedByMinutes() {
        // given
        Instant startsAt = Instant.parse("2030-06-03T11:05:00Z");
        Instant endsAt = Instant.parse("2030-06-03T12:00:00Z");

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ALIGNED_TO_SLOT);
    }

    @Test
    void shouldRejectStartNotAlignedBySeconds() {
        // given
        Instant startsAt = Instant.parse("2030-06-03T11:00:30Z");
        Instant endsAt = Instant.parse("2030-06-03T12:00:00Z");

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ALIGNED_TO_SLOT);
    }

    @ParameterizedTest(name = "starts at {0}")
    @ValueSource(strings = {
            "2030-06-03T11:00:00.001Z",
            "2030-06-03T11:00:00.000001Z",
            "2030-06-03T11:00:00.000000001Z"})
    void shouldRejectStartWithFractionOfSecond(String start) {
        // given
        Instant startsAt = Instant.parse(start);
        Instant endsAt = Instant.parse("2030-06-03T12:00:00Z");

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ALIGNED_TO_SLOT);
    }

    @Test
    void shouldRejectEndNotAlignedByMinutes() {
        // given
        Instant startsAt = Instant.parse("2030-06-03T11:00:00Z");
        Instant endsAt = Instant.parse("2030-06-03T12:10:00Z");

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ALIGNED_TO_SLOT);
    }

    @Test
    void shouldRejectEndNotAlignedBySeconds() {
        // given
        Instant startsAt = Instant.parse("2030-06-03T11:00:00Z");
        Instant endsAt = Instant.parse("2030-06-03T12:00:30Z");

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ALIGNED_TO_SLOT);
    }

    @ParameterizedTest(name = "ends at {0}")
    @ValueSource(strings = {
            "2030-06-03T12:00:00.001Z",
            "2030-06-03T12:00:00.000001Z",
            "2030-06-03T12:00:00.000000001Z"})
    void shouldRejectEndWithFractionOfSecond(String end) {
        // given
        Instant startsAt = Instant.parse("2030-06-03T11:00:00Z");
        Instant endsAt = Instant.parse(end);

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ALIGNED_TO_SLOT);
    }

    // --- duration from 15 minutes to 8 hours, inclusive ---

    @Test
    void shouldAcceptExactlyFifteenMinutes() {
        // given
        Instant startsAt = NOW.plus(Duration.ofHours(1));
        Instant endsAt = startsAt.plus(Duration.ofMinutes(15));

        // when / then
        assertThatCode(() -> rules.check(startsAt, endsAt)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptExactlyEightHours() {
        // given
        Instant startsAt = NOW.plus(Duration.ofHours(1));
        Instant endsAt = startsAt.plus(Duration.ofHours(8));

        // when / then
        assertThatCode(() -> rules.check(startsAt, endsAt)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectEightHoursAndFifteenMinutes() {
        // given
        Instant startsAt = NOW.plus(Duration.ofHours(1));
        Instant endsAt = startsAt.plus(Duration.ofHours(8).plusMinutes(15));

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(DURATION_RANGE);
    }

    @Test
    void shouldRejectBookingSpanningSeveralDays() {
        // given
        Instant startsAt = NOW.plus(Duration.ofHours(1));
        Instant endsAt = startsAt.plus(Duration.ofDays(3));

        // when / then
        assertThatThrownBy(() -> rules.check(startsAt, endsAt))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(DURATION_RANGE);
    }

    private static BookingTimeRules rulesAt(Instant now) {
        return new BookingTimeRules(Clock.fixed(now, ZoneOffset.UTC));
    }
}
