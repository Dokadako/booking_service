package com.dokadako.booking.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Loads the room in the same query, because the response includes the room name. */
    @EntityGraph(attributePaths = "room")
    Page<Booking> findByUserId(Long userId, Pageable pageable);
}
