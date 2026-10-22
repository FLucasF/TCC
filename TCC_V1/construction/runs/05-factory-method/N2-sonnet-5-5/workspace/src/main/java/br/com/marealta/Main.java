package br.com.marealta;

import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        Database db = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.migrate();
        IdentityTypes identityTypes = IdentityTypes.standard();
        RoomCatalog rooms = new RoomCatalog();
        Javalin app = build(rooms, identityTypes, new JdbcReservationRepository(db, rooms, identityTypes),
                new JdbcMessageRepository(db));
        String port = System.getenv("PORT");
        app.start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    public static Javalin build(RoomCatalog rooms, IdentityTypes identityTypes, ReservationRepository reservations,
                                MessageRepository messages) {
        ReservationService service = new ReservationService(rooms, new PriceCalculator(), reservations, messages);
        ReportService reports = new ReportService(reservations, rooms);
        return new Api(rooms, identityTypes, service, reports, messages).create();
    }
}
