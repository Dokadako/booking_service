package com.dokadako.booking.room;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dokadako.booking.common.ConflictException;
import com.dokadako.booking.common.NotFoundException;
import com.dokadako.booking.common.PageRequests;

@Service
class RoomService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "id", "id",
            "name", "name",
            "capacity", "capacity");

    private final RoomRepository roomRepository;

    RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    Page<RoomDtos.RoomResponse> findAll(Pageable pageable) {
        return roomRepository.findAll(PageRequests.withAllowedSort(pageable, SORTABLE_FIELDS))
                .map(RoomDtos.RoomResponse::from);
    }

    @Transactional(readOnly = true)
    RoomDtos.RoomResponse findById(long id) {
        return RoomDtos.RoomResponse.from(getRoom(id));
    }

    @Transactional
    RoomDtos.RoomResponse create(RoomDtos.RoomRequest request) {
        if (roomRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Room with name '%s' already exists".formatted(request.name()));
        }
        Room room = roomRepository.save(new Room(request.name(), request.capacity(), request.description()));
        return RoomDtos.RoomResponse.from(room);
    }

    @Transactional
    RoomDtos.RoomResponse update(long id, RoomDtos.RoomRequest request) {
        Room room = getRoom(id);
        if (roomRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Room with name '%s' already exists".formatted(request.name()));
        }
        room.update(request.name(), request.capacity(), request.description());
        return RoomDtos.RoomResponse.from(room);
    }

    @Transactional
    void delete(long id) {
        roomRepository.delete(getRoom(id));
    }

    private Room getRoom(long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room %d not found".formatted(id)));
    }
}
