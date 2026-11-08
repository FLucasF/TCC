package pousada.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Uma transação aberta, com os repositórios que trabalham nela. */
public class Tx {
    private static final int SCHEMA_LOCK_KEY = 1;
    private static final int ROOM_LOCK_NAMESPACE = 2;

    private final Connection connection;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    Tx(Connection connection) {
        this.connection = connection;
        this.reservations = new ReservationRepository(connection);
        this.messages = new MessageRepository(connection);
    }

    public ReservationRepository reservations() {
        return reservations;
    }

    public MessageRepository messages() {
        return messages;
    }

    Connection connection() {
        return connection;
    }

    /** Serializa quem mexe nas reservas de um quarto até o fim da transação. */
    public void lockRoom(int roomNumber) {
        advisoryLock(ROOM_LOCK_NAMESPACE, roomNumber);
    }

    void lockSchemaCreation() {
        advisoryLock(SCHEMA_LOCK_KEY, 0);
    }

    private void advisoryLock(int namespace, int key) {
        try (PreparedStatement st = connection.prepareStatement("SELECT pg_advisory_xact_lock(?, ?)")) {
            st.setInt(1, namespace);
            st.setInt(2, key);
            st.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }
}
