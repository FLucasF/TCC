package br.com.marealta.pousada;

import java.time.Clock;

public class Main {
    public static void main(String[] args) {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) throw new IllegalStateException("DB_URL não definida");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Db db = new Db(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.init();

        MailConfig mailConfig = MailConfig.fromEnv(System.getenv());
        MailDispatcher mail = new MailDispatcher(mailConfig, Clock.systemUTC());
        ReservationService service = new ReservationService(db, mail);
        Api api = new Api(service);
        api.build().start(port);
        Runtime.getRuntime().addShutdownHook(new Thread(db::close));
    }
}
