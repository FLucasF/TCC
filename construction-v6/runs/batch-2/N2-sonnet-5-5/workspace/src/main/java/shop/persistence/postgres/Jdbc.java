package shop.persistence.postgres;

import java.sql.SQLException;

final class Jdbc {
    interface Call<T> {
        T run() throws SQLException;
    }

    private Jdbc() {
    }

    static <T> T call(Call<T> call) {
        try {
            return call.run();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
