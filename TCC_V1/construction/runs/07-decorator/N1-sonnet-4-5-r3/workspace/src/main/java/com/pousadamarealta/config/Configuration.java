package com.pousadamarealta.config;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.messaging.MessagePipeline;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRedirectRule;
import com.pousadamarealta.repository.DatabaseConnection;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.MessagingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.web.WebApplication;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    public static Javalin createApplication() {
        String dbUrl = getEnv("DB_URL", null);
        String dbUser = getEnv("DB_USER", null);
        String dbPassword = getEnv("DB_PASSWORD", null);
        boolean testMode = Boolean.parseBoolean(getEnv("HOTEL_TEST_MODE", "false"));

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database configuration not provided");
        }

        DatabaseConnection.initialize(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        initializeRooms(roomRepository);

        MessagePipeline messagePipeline = createMessagePipeline(testMode);
        MessagingService messagingService = new MessagingService(messageRepository, messagePipeline);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messagingService);

        WebApplication webApp = new WebApplication(
            reservationService, messagingService, reportService, roomRepository);
        return webApp.createApp();
    }

    private static MessagePipeline createMessagePipeline(boolean testMode) {
        List<MessageRule> rules = new ArrayList<>();

        rules.add(new EmailValidationRule());

        if (testMode) {
            rules.add(new TestModeRedirectRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }

        return new MessagePipeline(rules);
    }

    private static void initializeRooms(RoomRepository roomRepository) {
        roomRepository.save(new Room("101", RoomType.STANDARD));
        roomRepository.save(new Room("102", RoomType.STANDARD));
        roomRepository.save(new Room("201", RoomType.DELUXE));
        roomRepository.save(new Room("202", RoomType.DELUXE));
        roomRepository.save(new Room("301", RoomType.SUITE));
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    public static int getPort() {
        String port = getEnv("PORT", "7070");
        return Integer.parseInt(port);
    }
}
