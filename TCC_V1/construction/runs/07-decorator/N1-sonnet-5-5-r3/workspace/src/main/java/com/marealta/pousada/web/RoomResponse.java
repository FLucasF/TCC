package com.marealta.pousada.web;

import com.marealta.pousada.domain.Room;
import com.marealta.pousada.domain.RoomType;

import java.math.BigDecimal;

record RoomResponse(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
    static RoomResponse of(Room room) {
        return new RoomResponse(room.number(), room.type(), room.capacity(), room.nightlyRate());
    }
}
