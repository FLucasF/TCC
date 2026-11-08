package pousada.service;

import pousada.domain.Message;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    /** Guarda a reserva nova e devolve com o id; ROOM_UNAVAILABLE se o quarto já estiver ocupado numa das noites. */
    Reservation insert(Reservation draft);

    Optional<Reservation> find(long id);

    /**
     * Grava o novo estado da reserva (e as mensagens, na mesma operação) se ela ainda estiver em {@code expected};
     * senão, OPERATION_NOT_ALLOWED.
     */
    void update(Reservation reservation, ReservationStatus expected, List<Message> messages);

    List<Message> messages();

    /** Reservas pagas que ocupam um quarto na noite. */
    List<Reservation> paidOnNight(LocalDate night);

    /** Reservas cuja entrada está no intervalo [from, to]. */
    List<Reservation> checkInBetween(LocalDate from, LocalDate to);
}
