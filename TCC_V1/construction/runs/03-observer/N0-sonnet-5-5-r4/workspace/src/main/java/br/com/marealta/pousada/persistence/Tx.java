package br.com.marealta.pousada.persistence;

import java.sql.Connection;

/** Uma transação aberta, com os repositórios que trabalham dentro dela. */
public final class Tx {
    private final Connection connection;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    Tx(Connection connection) {
        this.connection = connection;
        this.rooms = new RoomRepository(connection);
        this.reservations = new ReservationRepository(connection);
        this.messages = new MessageRepository(connection);
    }

    Connection connection() {
        return connection;
    }

    public RoomRepository rooms() {
        return rooms;
    }

    public ReservationRepository reservations() {
        return reservations;
    }

    public MessageRepository messages() {
        return messages;
    }
}
