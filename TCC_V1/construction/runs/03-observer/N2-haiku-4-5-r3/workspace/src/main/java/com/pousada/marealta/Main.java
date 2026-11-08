package com.pousada.marealta;

import com.pousada.marealta.infrastructure.database.DatabaseSetup;
import com.pousada.marealta.infrastructure.persistence.postgres.PostgresNotificationRepository;
import com.pousada.marealta.infrastructure.persistence.postgres.PostgresReservationRepository;
import com.pousada.marealta.infrastructure.persistence.postgres.PostgresRoomRepository;
import com.pousada.marealta.infrastructure.web.GuesthouseApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            // Setup database
            DataSource dataSource = DatabaseSetup.createDataSource();
            DatabaseSetup.runMigrations(dataSource);

            // Initialize repositories
            var roomRepository = new PostgresRoomRepository(dataSource);
            var reservationRepository = new PostgresReservationRepository(dataSource, roomRepository);
            var notificationRepository = new PostgresNotificationRepository(dataSource);

            // Create and start API
            GuesthouseApi api = new GuesthouseApi(roomRepository, reservationRepository, notificationRepository);

            int port = getPort();
            api.start(port);

            logger.info("Guesthouse API started successfully on port {}", port);
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                logger.warn("Invalid PORT environment variable: {}, using default 7070", portEnv);
            }
        }
        return 7070;
    }
}
