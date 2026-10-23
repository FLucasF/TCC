package com.pousada.domain.repository;

import com.pousada.domain.model.Room;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    List<Room> findAll();
    Optional<Room> findByNumber(String number);
}
