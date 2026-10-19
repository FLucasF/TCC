package br.com.marealta.dominio;

/**
 * Quem quer saber o que acontece com as reservas. Para avisar um novo interessado, basta criar uma
 * implementação e registrá-la na montagem da aplicação; cada uma sobrescreve só os eventos que lhe interessam.
 */
public interface OuvinteReserva {

    /** A reserva acabou de ser confirmada (paga). */
    default void reservaConfirmada(Reserva reserva) {
    }

    /** Uma reserva que já estava confirmada acabou de ser cancelada; o reembolso já está definido. */
    default void reservaConfirmadaCancelada(Reserva reserva) {
    }
}
