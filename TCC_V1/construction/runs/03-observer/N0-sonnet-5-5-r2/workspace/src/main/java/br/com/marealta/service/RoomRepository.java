package br.com.marealta.service;

import br.com.marealta.domain.Room;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    List<Room> findAll();

    Optional<Room> find(int number);

    /** Busca o quarto travando-o até o fim da transação, para serializar reservas concorrentes. */
    Optional<Room> findForUpdate(int number);
}
