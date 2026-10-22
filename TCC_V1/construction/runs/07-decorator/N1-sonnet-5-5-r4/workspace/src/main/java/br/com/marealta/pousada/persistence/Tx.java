package br.com.marealta.pousada.persistence;

import java.sql.Connection;

/** O que se pode acessar dentro de uma transação. */
public final class Tx {
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    Tx(Connection connection) {
        this.reservations = new ReservationRepository(connection);
        this.messages = new MessageRepository(connection);
    }

    public ReservationRepository reservations() {
        return reservations;
    }

    public MessageRepository messages() {
        return messages;
    }
}
