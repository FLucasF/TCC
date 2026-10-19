package com.pousadamarealta.api.mapper;

import com.pousadamarealta.api.dto.RoomDTO;
import com.pousadamarealta.domain.Room;

public class RoomMapper {
    public static RoomDTO toDTO(Room room) {
        return new RoomDTO(
            room.getNumber(),
            room.getType().toString(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }
}
