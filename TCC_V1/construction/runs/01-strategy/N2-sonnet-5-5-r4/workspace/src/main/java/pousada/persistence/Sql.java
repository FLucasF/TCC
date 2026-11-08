package pousada.persistence;

import java.sql.SQLException;

final class Sql {
    private Sql() {}

    interface Action<T> { T run() throws SQLException; }

    static <T> T unchecked(Action<T> action) {
        try {
            return action.run();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
