package com.pousada.repository;

import com.pousada.domain.Reservation;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface ReservationRepository {

    /**
     * Grava o rascunho com o próximo número (o número do rascunho é ignorado) e devolve a reserva, se o quarto estiver livre em todas as noites.
     * Lança ROOM_UNAVAILABLE se não estiver. A verificação e a gravação são atômicas.
     */
    Reservation create(Reservation draft);

    Optional<Reservation> find(long id);

    /** Aplica a mudança com a reserva travada e grava o resultado; RESERVATION_NOT_FOUND se não existir. */
    Reservation update(long id, Consumer<Reservation> change);

    List<Reservation> findAll();
}
