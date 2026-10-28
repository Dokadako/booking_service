package com.dokadako.booking.booking;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dokadako.booking.common.NotFoundException;
import com.dokadako.booking.common.PageRequests;
import com.dokadako.booking.room.Room;
import com.dokadako.booking.room.RoomRepository;
import com.dokadako.booking.user.User;
import com.dokadako.booking.user.UserRepository;

@Service
class BookingService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "startsAt", "startsAt",
            "endsAt", "endsAt",
            "createdAt", "createdAt",
            "status", "status",
            "roomName", "room.name");

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final BookingTimeRules timeRules;
    private final Clock clock;

    BookingService(BookingRepository bookingRepository, RoomRepository roomRepository,
                   UserRepository userRepository, BookingTimeRules timeRules, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.timeRules = timeRules;
        this.clock = clock;
    }

    @Transactional
    BookingDtos.BookingResponse create(long userId, BookingDtos.CreateBookingRequest request) {
        timeRules.check(request.startsAt(), request.endsAt());
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new NotFoundException("Room %d not found".formatted(request.roomId())));
        // The id comes from a verified token, so a reference is enough: no extra SELECT for the user.
        User user = userRepository.getReferenceById(userId);
        Booking booking = bookingRepository.save(
                new Booking(room, user, request.startsAt(), request.endsAt(), Instant.now(clock)));
        return BookingDtos.BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    Page<BookingDtos.BookingResponse> findByUser(long userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, PageRequests.withAllowedSort(pageable, SORTABLE_FIELDS))
                .map(BookingDtos.BookingResponse::from);
    }
}
