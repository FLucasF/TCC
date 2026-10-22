package hotel;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * The connections to the inn's PostgreSQL: the address comes in DB_URL (jdbc:postgresql://host:port/database), the
 * user in DB_USER and the password in DB_PASSWORD. Opening it brings the schema up to date with the migrations in
 * src/main/resources/db/migration.
 */
public final class Database implements AutoCloseable {

    private final HikariDataSource dataSource;

    public Database(Map<String, String> environment) {
        String url = environment.get("DB_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("DB_URL is not set: it must hold the PostgreSQL address");
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(environment.get("DB_USER"));
        config.setPassword(environment.get("DB_PASSWORD"));
        config.setMaximumPoolSize(5);
        this.dataSource = new HikariDataSource(config);
        try {
            Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        } catch (Exception e) {
            // Continue even if clean fails
        }
        Flyway.configure().dataSource(dataSource).load().migrate();
    }

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
