package com.pousada.marealta.domain.repository;

import com.pousada.marealta.domain.room.Room;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    List<Room> findAll();
    Optional<Room> findByNumber(int number);
    void save(Room room);
}
