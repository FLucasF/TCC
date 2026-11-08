package br.com.pousada.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    public static void initializeDatabase() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Variáveis de ambiente DB_URL, DB_USER e DB_PASSWORD são obrigatórias");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);
        logger.info("Pool de conexões inicializado");

        createSchemaIfNotExists();
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static void createSchemaIfNotExists() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = """
                CREATE TABLE IF NOT EXISTS rooms (
                    number INT PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    capacity INT NOT NULL,
                    nightly_rate NUMERIC(10, 2) NOT NULL
                );

                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number INT NOT NULL REFERENCES rooms(number),
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(14) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INT NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights BIGINT NOT NULL,
                    subtotal NUMERIC(10, 2) NOT NULL,
                    discount NUMERIC(10, 2) NOT NULL,
                    total NUMERIC(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                    refund NUMERIC(10, 2),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                );

                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    "to" VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                );

                CREATE INDEX IF NOT EXISTS idx_reservations_room ON reservations(room_number);
                CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
                CREATE INDEX IF NOT EXISTS idx_reservations_check_in ON reservations(check_in);
                """;

            stmt.execute(sql);
            logger.info("Schema criado com sucesso");

            insertInitialRooms();

        } catch (SQLException e) {
            logger.error("Erro ao criar schema", e);
            throw new RuntimeException(e);
        }
    }

    private static void insertInitialRooms() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String checkSql = "SELECT COUNT(*) as cnt FROM rooms";
            var rs = stmt.executeQuery(checkSql);
            rs.next();
            if (rs.getInt("cnt") == 0) {
                String insertSql = """
                    INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
                    (101, 'STANDARD', 2, 249.90),
                    (102, 'STANDARD', 2, 249.90),
                    (201, 'DELUXE', 3, 389.90),
                    (202, 'DELUXE', 3, 389.90),
                    (301, 'SUITE', 4, 579.90);
                    """;
                stmt.execute(insertSql);
                logger.info("Quartos iniciais inseridos");
            }
        } catch (SQLException e) {
            logger.error("Erro ao inserir quartos iniciais", e);
            throw new RuntimeException(e);
        }
    }
}
