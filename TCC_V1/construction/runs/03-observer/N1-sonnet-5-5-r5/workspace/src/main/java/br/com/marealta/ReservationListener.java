package br.com.marealta;

import java.util.List;

/**
 * Quem quer ser avisado do que acontece com as reservas. Cada interessado implementa só os eventos
 * que lhe importam e devolve as mensagens a enviar. Novos interessados entram em {@link App}; novos
 * eventos entram aqui, como mais um método sem efeito por padrão.
 */
public interface ReservationListener {
    /** A reserva acabou de ser confirmada (paga). */
    default List<Message> confirmed(Reservation reservation) { return List.of(); }

    /** Uma reserva que já estava confirmada acabou de ser cancelada. */
    default List<Message> cancelledAfterConfirmation(Reservation reservation) { return List.of(); }
}
