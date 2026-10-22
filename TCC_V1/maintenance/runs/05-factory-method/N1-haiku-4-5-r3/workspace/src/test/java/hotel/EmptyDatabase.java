package hotel;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

/**
 * The database the tests use, in the same variables as the service (DB_URL, DB_USER, DB_PASSWORD), emptied when
 * opened: no reservation, no message, and the numbering back to 1. The rooms stay.
 */
public final class EmptyDatabase implements AutoCloseable {

    private final Database database = new Database(System.getenv());

    public EmptyDatabase() {
        try (Connection connection = database.dataSource().getConnection();
                Statement statement = connection.createStatement()) {
            try {
                statement.execute("TRUNCATE reservations, messages RESTART IDENTITY CASCADE");
            } catch (SQLException e) {
                // Table might not exist after migrations, which is OK
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not connect to test database", failure);
        }
    }

    public DataSource dataSource() {
        return database.dataSource();
    }

    @Override
    public void close() {
        database.close();
    }
}
