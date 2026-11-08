package com.pousada.marealta.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseSetup {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSetup.class);

    public static DataSource createDataSource() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUrl.isBlank()) {
            throw new RuntimeException("DB_URL environment variable not set");
        }
        if (dbUser == null || dbUser.isBlank()) {
            throw new RuntimeException("DB_USER environment variable not set");
        }
        if (dbPassword == null || dbPassword.isBlank()) {
            throw new RuntimeException("DB_PASSWORD environment variable not set");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        logger.info("Creating database connection pool to {}", dbUrl);
        return new HikariDataSource(config);
    }

    public static void runMigrations(DataSource dataSource) {
        logger.info("Running database migrations");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .load();
        flyway.migrate();
        logger.info("Database migrations completed");
    }
}
