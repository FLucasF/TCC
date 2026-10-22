package com.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.persistence.Database;
import com.pousada.persistence.MessageRepository;
import com.pousada.persistence.ReservationRepository;
import com.pousada.persistence.RoomRepository;
import com.pousada.plan.RatePlans;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import com.pousada.web.Routes;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {

    public static void main(String[] args) {
        Database db = new Database(required("DB_URL"), required("DB_USER"), required("DB_PASSWORD"));
        db.initialize();

        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservations = new ReservationRepository();
        ReservationService service = new ReservationService(db, rooms, reservations, new MessageRepository(),
                RatePlans.defaults());
        ReportService reports = new ReportService(db, rooms, reservations);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));
        new Routes(service, reports, mapper).register(app);

        String port = System.getenv("PORT");
        app.start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return value;
    }
}
