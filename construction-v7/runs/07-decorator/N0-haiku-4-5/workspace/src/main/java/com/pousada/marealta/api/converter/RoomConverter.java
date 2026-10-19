package com.pousada.marealta.api.converter;

import com.pousada.marealta.api.dto.RoomDto;
import com.pousada.marealta.model.Room;

public class RoomConverter {
    public static RoomDto toDto(Room room) {
        return new RoomDto(
            room.number(),
            room.type().toString(),
            room.capacity(),
            room.nightlyRate()
        );
    }
}
