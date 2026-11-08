package br.com.marealta.repository;

import br.com.marealta.domain.Room;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    List<Room> findAll();
    Optional<Room> findByNumber(int number);
    void initialize();
}
