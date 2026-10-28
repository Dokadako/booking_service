package com.dokadako.booking.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.dokadako.booking.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;

class BookingIntegrationTest extends AbstractIntegrationTest {

    /** The wire format of an Instant on a whole second: ISO-8601 in UTC, no fraction, "Z" suffix. */
    private static final DateTimeFormatter WIRE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    // --- create ---

    @Test
    void shouldCreateBookingForAuthenticatedUser() throws Exception {
        // given
        String roomName = uniqueRoomName();
        long roomId = createRoom(adminToken(), roomName);
        String user = registerAndLogin(uniqueEmail());
        Instant startsAt = tomorrow().plus(Duration.ofHours(1));
        Instant endsAt = startsAt.plus(Duration.ofMinutes(90));

        // when / then
        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(roomId, startsAt, endsAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.roomName").value(roomName))
                .andExpect(jsonPath("$.startsAt").value(WIRE_FORMAT.format(startsAt)))
                .andExpect(jsonPath("$.endsAt").value(WIRE_FORMAT.format(endsAt)))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void shouldReturnNotFoundForUnknownRoom() throws Exception {
        // given
        String user = registerAndLogin(uniqueEmail());
        Instant startsAt = tomorrow();

        // when / then
        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(Long.MAX_VALUE, startsAt, startsAt.plus(Duration.ofHours(1)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Room %d not found".formatted(Long.MAX_VALUE)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("timeRuleViolations")
    void shouldRejectBookingThatBreaksTimeRuleAndSaveNothing(String violation, Duration startOffset,
                                                            Duration endOffset, String expectedDetail)
            throws Exception {
        // given
        long roomId = createRoom(adminToken(), uniqueRoomName());
        String user = registerAndLogin(uniqueEmail());
        Instant base = tomorrow();

        // when / then
        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(roomId, base.plus(startOffset), base.plus(endOffset))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(expectedDetail));

        mockMvc.perform(get("/api/bookings/my").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    static Stream<Arguments> timeRuleViolations() {
        return Stream.of(
                Arguments.of("starts in the past",
                        Duration.ofDays(-2), Duration.ofDays(-2).plusHours(1),
                        "Booking must start in the future"),
                Arguments.of("ends before it starts",
                        Duration.ofHours(2), Duration.ofHours(1),
                        "Booking must start before it ends"),
                Arguments.of("not on a 15-minute boundary",
                        Duration.ofMinutes(10), Duration.ofMinutes(70),
                        "Booking must start and end on a 15-minute boundary"),
                Arguments.of("longer than 8 hours",
                        Duration.ZERO, Duration.ofHours(8).plusMinutes(15),
                        "Booking must last from 15 minutes to 8 hours"));
    }

    @Test
    void shouldRejectRequestWithAllFieldsMissing() throws Exception {
        // given
        String user = registerAndLogin(uniqueEmail());

        // when / then
        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.roomId").exists())
                .andExpect(jsonPath("$.errors.startsAt").exists())
                .andExpect(jsonPath("$.errors.endsAt").exists());
    }

    @Test
    void shouldReportOnlyTheMissingField() throws Exception {
        // given
        long roomId = createRoom(adminToken(), uniqueRoomName());
        String user = registerAndLogin(uniqueEmail());

        // when / then
        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId": %d, "startsAt": "%s"}
                                """.formatted(roomId, tomorrow())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.endsAt").exists())
                .andExpect(jsonPath("$.errors.roomId").doesNotExist())
                .andExpect(jsonPath("$.errors.startsAt").doesNotExist());
    }

    @Test
    void shouldRejectUnparseableTimestamp() throws Exception {
        // given
        long roomId = createRoom(adminToken(), uniqueRoomName());
        String user = registerAndLogin(uniqueEmail());

        // when / then
        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId": %d, "startsAt": "tomorrow at nine", "endsAt": "%s"}
                                """.formatted(roomId, tomorrow())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCreatingBookingWithoutToken() throws Exception {
        // given
        Instant startsAt = tomorrow();

        // when / then
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(1L, startsAt, startsAt.plus(Duration.ofHours(1)))))
                .andExpect(status().isUnauthorized());
    }

    // --- list my bookings ---

    @Test
    void shouldListOnlyTheCallersOwnBookings() throws Exception {
        // given
        long roomId = createRoom(adminToken(), uniqueRoomName());
        String alice = registerAndLogin(uniqueEmail());
        String bob = registerAndLogin(uniqueEmail());
        Instant base = tomorrow();
        long aliceBooking = createBooking(alice, roomId, base, base.plus(Duration.ofHours(1)));
        long bobBooking = createBooking(bob, roomId, base.plus(Duration.ofHours(2)), base.plus(Duration.ofHours(3)));

        // when / then
        mockMvc.perform(get("/api/bookings/my").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(aliceBooking))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get("/api/bookings/my").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(bobBooking))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void shouldReturnEmptyPageWhenUserHasNoBookings() throws Exception {
        // given
        String user = registerAndLogin(uniqueEmail());

        // when / then
        mockMvc.perform(get("/api/bookings/my").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void shouldListOwnBookingsLatestStartFirstByDefault() throws Exception {
        // given: created in an order that differs from both id order and start order
        long roomId = createRoom(adminToken(), uniqueRoomName());
        String user = registerAndLogin(uniqueEmail());
        Instant base = tomorrow();
        long middle = createBooking(user, roomId, base.plus(Duration.ofDays(1)), base.plus(Duration.ofDays(1)).plus(Duration.ofHours(1)));
        long earliest = createBooking(user, roomId, base, base.plus(Duration.ofHours(1)));
        long latest = createBooking(user, roomId, base.plus(Duration.ofDays(2)), base.plus(Duration.ofDays(2)).plus(Duration.ofHours(1)));

        // when / then
        mockMvc.perform(get("/api/bookings/my").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].id").value(latest))
                .andExpect(jsonPath("$.content[1].id").value(middle))
                .andExpect(jsonPath("$.content[2].id").value(earliest));
    }

    @Test
    void shouldApplyRequestedSortAndPageSize() throws Exception {
        // given
        long roomId = createRoom(adminToken(), uniqueRoomName());
        String user = registerAndLogin(uniqueEmail());
        Instant base = tomorrow();
        long middle = createBooking(user, roomId, base.plus(Duration.ofDays(1)), base.plus(Duration.ofDays(1)).plus(Duration.ofHours(1)));
        long earliest = createBooking(user, roomId, base, base.plus(Duration.ofHours(1)));
        createBooking(user, roomId, base.plus(Duration.ofDays(2)), base.plus(Duration.ofDays(2)).plus(Duration.ofHours(1)));

        // when / then
        mockMvc.perform(get("/api/bookings/my")
                        .param("sort", "startsAt,asc")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(earliest))
                .andExpect(jsonPath("$.content[1].id").value(middle))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void shouldRejectUnknownSortProperty() throws Exception {
        // given
        String user = registerAndLogin(uniqueEmail());

        // when / then
        mockMvc.perform(get("/api/bookings/my")
                        .param("sort", "doesNotExist")
                        .header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectListingBookingsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/bookings/my"))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private long createBooking(String token, long roomId, Instant startsAt, Instant endsAt) throws Exception {
        String body = mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(roomId, startsAt, endsAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String bookingJson(long roomId, Instant startsAt, Instant endsAt) {
        return """
                {"roomId": %d, "startsAt": "%s", "endsAt": "%s"}
                """.formatted(roomId, startsAt, endsAt);
    }

    /**
     * The start of the current hour plus one day: always in the future and on a 15-minute boundary.
     * Call it once per test and derive every other time from it, so all times share one base.
     */
    private static Instant tomorrow() {
        return Instant.now().truncatedTo(ChronoUnit.HOURS).plus(Duration.ofDays(1));
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    private static String uniqueRoomName() {
        return "Room " + UUID.randomUUID();
    }
}
