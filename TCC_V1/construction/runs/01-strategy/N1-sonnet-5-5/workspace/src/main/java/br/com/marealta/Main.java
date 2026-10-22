package br.com.marealta;

import br.com.marealta.infra.Db;
import br.com.marealta.infra.MessageRepository;
import br.com.marealta.infra.ReservationRepository;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.Api;

public class Main {

    public static void main(String[] args) {
        Db db = new Db(requireEnv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        ReservationRepository reservations = new ReservationRepository();
        Api api = new Api(
                new ReservationService(db, reservations, new MessageRepository()),
                new ReportService(db, reservations));

        String port = System.getenv("PORT");
        api.create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return value;
    }
}
