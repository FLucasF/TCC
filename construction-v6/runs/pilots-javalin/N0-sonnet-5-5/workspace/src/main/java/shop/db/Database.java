package shop.db;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public final class Database {
    @FunctionalInterface
    public interface Work<T> {
        T run(Dao dao) throws SQLException;
    }

    private final DataSource dataSource;

    private final boolean postgres;

    public Database(DataSource dataSource) {
        this.dataSource = dataSource;
        try (Connection c = dataSource.getConnection()) {
            this.postgres = c.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres");
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    /** Runs the work in one transaction: commits if it returns, rolls back if it throws. */
    public <T> T tx(Work<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(new Dao(connection, postgres));
                connection.commit();
                return result;
            } catch (Throwable e) {
                try {
                    connection.rollback();
                } catch (SQLException suppressed) {
                    e.addSuppressed(suppressed);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }
}
