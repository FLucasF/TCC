package com.marealta;

import com.marealta.mail.MailPipeline;
import com.marealta.mail.MailSettings;
import com.marealta.mail.Outbox;
import com.marealta.persistence.Database;
import com.marealta.persistence.MessageLog;
import com.marealta.persistence.ReservationRepository;
import com.marealta.persistence.RoomRepository;
import com.marealta.report.Reports;
import com.marealta.service.ReservationService;
import com.marealta.web.Api;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        Database database = new Database(required("DB_URL"), required("DB_USER"), required("DB_PASSWORD"));
        database.migrate();

        MailSettings settings = new MailSettings(
                "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE")),
                !"false".equalsIgnoreCase(System.getenv("HOTEL_ARCHIVE_COPY")));
        MessageLog log = new MessageLog(database);
        Outbox outbox = MailPipeline.build(log, settings);

        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservations = new ReservationRepository();
        ReservationService service = new ReservationService(database, rooms, reservations, outbox);
        Reports reports = new Reports(database, rooms, reservations);

        String port = System.getenv("PORT");
        new Api(service, log, reports).create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return value;
    }
}
