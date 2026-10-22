package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    List<Room> findAll();
    Optional<Room> findByNumber(String number);
}
