package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;

import java.util.List;

/**
 * Quem quer ser avisado do que acontece com as reservas. Cada interessado é uma classe que implementa só os
 * acontecimentos que lhe importam; para incluir ou tirar um, basta mexer na lista em Main.
 * Um novo tipo de acontecimento entra aqui como mais um método com resposta vazia por padrão.
 */
public interface ReservationListener {

    /** A reserva acabou de ser confirmada (paga). */
    default List<Message> onConfirmed(Reservation reservation, Room room) {
        return List.of();
    }

    /** Uma reserva que já estava confirmada acabou de ser cancelada; {@code reservation.refund()} está preenchido. */
    default List<Message> onPaidReservationCancelled(Reservation reservation, Room room) {
        return List.of();
    }
}
