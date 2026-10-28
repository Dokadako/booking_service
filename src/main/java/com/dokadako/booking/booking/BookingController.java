package com.dokadako.booking.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
class BookingController {

    private final BookingService bookingService;

    BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookingDtos.BookingResponse create(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody BookingDtos.CreateBookingRequest request) {
        return bookingService.create(currentUserId(jwt), request);
    }

    @GetMapping("/my")
    Page<BookingDtos.BookingResponse> findMine(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "startsAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return bookingService.findByUser(currentUserId(jwt), pageable);
    }

    /** The token subject is the user id, set by TokenService when the token was issued. */
    private static long currentUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
