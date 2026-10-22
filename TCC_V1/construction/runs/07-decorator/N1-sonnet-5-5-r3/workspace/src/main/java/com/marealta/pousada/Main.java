package com.marealta.pousada;

import com.marealta.pousada.domain.ReportService;
import com.marealta.pousada.domain.ReservationService;
import com.marealta.pousada.domain.RoomCatalog;
import com.marealta.pousada.mail.MailRules;
import com.marealta.pousada.mail.MailService;
import com.marealta.pousada.persistence.*;
import com.marealta.pousada.web.Api;

import javax.sql.DataSource;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        DataSource dataSource = Database.connect(System.getenv("DB_URL"), System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD"));

        ReservationRepository reservationRepository = new JdbcReservationRepository(dataSource);
        MessageRepository messageRepository = new JdbcMessageRepository(dataSource);
        RoomCatalog rooms = new RoomCatalog();
        MailService mail = new MailService(messageRepository, MailRules.marealta(), testMode);
        new Api(new ReservationService(reservationRepository, rooms, mail),
                new ReportService(reservationRepository, rooms), mail, rooms).create().start(port);
    }
}
