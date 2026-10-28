package com.dokadako.booking.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

final class RoomDtos {

    private RoomDtos() {
    }

    record RoomRequest(
            @NotBlank @Size(max = 100) String name,
            @Min(1) @Max(500) int capacity,
            @Size(max = 500) String description) {
    }

    record RoomResponse(Long id, String name, int capacity, String description) {

        static RoomResponse from(Room room) {
            return new RoomResponse(room.getId(), room.getName(), room.getCapacity(), room.getDescription());
        }
    }
}
