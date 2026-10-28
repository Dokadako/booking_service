package com.dokadako.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dokadako.booking.common.BadRequestException;
import com.dokadako.booking.common.NotFoundException;
import com.dokadako.booking.room.Room;
import com.dokadako.booking.room.RoomRepository;
import com.dokadako.booking.user.Role;
import com.dokadako.booking.user.User;
import com.dokadako.booking.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Instant NOW = Instant.parse("2030-06-03T10:00:00Z");
    private static final Instant STARTS_AT = NOW.plus(Duration.ofDays(1));
    private static final Instant ENDS_AT = STARTS_AT.plus(Duration.ofHours(1));

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        bookingService = new BookingService(bookingRepository, roomRepository, userRepository,
                new BookingTimeRules(clock), clock);
    }

    @Test
    void shouldSaveConfirmedBookingWithCreationTimeFromClock() {
        // given
        Room room = new Room("Blue", 6, null);
        User user = new User("user@test.local", "hash", Role.USER, NOW);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        BookingDtos.BookingResponse response =
                bookingService.create(7L, new BookingDtos.CreateBookingRequest(1L, STARTS_AT, ENDS_AT));

        // then
        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertThat(saved.getValue().getRoom()).isSameAs(room);
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.roomName()).isEqualTo("Blue");
        assertThat(response.startsAt()).isEqualTo(STARTS_AT);
        assertThat(response.endsAt()).isEqualTo(ENDS_AT);
    }

    @Test
    void shouldNotTouchDatabaseWhenTimeRuleIsBroken() {
        // given
        Instant pastStart = NOW.minus(Duration.ofHours(1));

        // when / then
        assertThatThrownBy(() -> bookingService.create(7L,
                new BookingDtos.CreateBookingRequest(1L, pastStart, pastStart.plus(Duration.ofHours(1)))))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(roomRepository, userRepository, bookingRepository);
    }

    @Test
    void shouldNotSaveBookingForUnknownRoom() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> bookingService.create(7L,
                new BookingDtos.CreateBookingRequest(1L, STARTS_AT, ENDS_AT)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Room 1 not found");
        verify(bookingRepository, never()).save(any());
    }
}
