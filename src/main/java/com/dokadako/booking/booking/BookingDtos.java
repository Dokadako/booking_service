package com.dokadako.booking.booking;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

final class BookingDtos {

    private BookingDtos() {
    }

    record CreateBookingRequest(
            @NotNull Long roomId,
            @NotNull Instant startsAt,
            @NotNull Instant endsAt) {
    }

    record BookingResponse(
            Long id,
            Long roomId,
            String roomName,
            Instant startsAt,
            Instant endsAt,
            BookingStatus status,
            Instant createdAt) {

        static BookingResponse from(Booking booking) {
            return new BookingResponse(booking.getId(), booking.getRoom().getId(), booking.getRoom().getName(),
                    booking.getStartsAt(), booking.getEndsAt(), booking.getStatus(), booking.getCreatedAt());
        }
    }
}
