package com.dokadako.booking.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dokadako.booking.common.ConflictException;
import com.dokadako.booking.common.NotFoundException;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    void shouldNotSaveRoomWhenNameIsTaken() {
        when(roomRepository.existsByNameIgnoreCase("Blue")).thenReturn(true);

        assertThatThrownBy(() -> roomService.create(new RoomDtos.RoomRequest("Blue", 4, null)))
                .isInstanceOf(ConflictException.class);
        verify(roomRepository, never()).save(any());
    }

    @Test
    void shouldAllowKeepingSameNameOnUpdate() {
        Room room = new Room("Blue", 4, null);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.existsByNameIgnoreCaseAndIdNot("Blue", 1L)).thenReturn(false);

        RoomDtos.RoomResponse response = roomService.update(1L, new RoomDtos.RoomRequest("Blue", 10, "Bigger"));

        assertThat(response.capacity()).isEqualTo(10);
        assertThat(response.description()).isEqualTo("Bigger");
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingRoom() {
        when(roomRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.update(42L, new RoomDtos.RoomRequest("Blue", 4, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Room 42 not found");
    }
}
