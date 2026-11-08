package pousada.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class Database {

    public static DataSource createDataSource() {
        String jdbcUrl = System.getenv("DB_URL");
        String username = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (jdbcUrl == null || username == null || password == null) {
            throw new IllegalStateException("Database configuration not found. Set DB_URL, DB_USER, and DB_PASSWORD environment variables.");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        return new HikariDataSource(config);
    }
}
