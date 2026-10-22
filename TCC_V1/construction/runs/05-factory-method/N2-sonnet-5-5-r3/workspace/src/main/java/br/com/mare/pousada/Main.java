package br.com.mare.pousada;

public class Main {
    public static void main(String[] args) {
        JdbcDatabase db = new JdbcDatabase(System.getenv("DB_URL"), System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD"));
        db.createSchema();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        start(new JdbcReservationRepository(db), new JdbcMessageRepository(db), port);
    }

    public static io.javalin.Javalin start(ReservationRepository reservations, MessageRepository messages,
                                           int port) {
        ReservationService service = new ReservationService(reservations, new Notifier(messages));
        return new Api(service, messages, new Reports(service)).start(port);
    }
}
