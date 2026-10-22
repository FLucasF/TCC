package br.com.mealta;

import br.com.mealta.domain.Pricing;
import br.com.mealta.domain.ReservationService;
import br.com.mealta.domain.RoomCatalog;
import br.com.mealta.report.ReportCatalog;
import br.com.mealta.store.Database;
import br.com.mealta.store.PgMessageLog;
import br.com.mealta.store.PgReservationRepository;
import br.com.mealta.store.Schema;
import br.com.mealta.web.Api;

import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        Database db = new Database(required("DB_URL"), required("DB_USER"), required("DB_PASSWORD"));
        Schema.create(db);

        RoomCatalog rooms = new RoomCatalog();
        PgReservationRepository reservations = new PgReservationRepository(db);
        PgMessageLog messages = new PgMessageLog(db);
        ReservationService service = new ReservationService(reservations, messages, db, rooms, new Pricing());
        // Valor da taxa de turismo por hóspede por noite; a prefeitura muda todo ano.
        BigDecimal tourismTax = new BigDecimal(System.getenv().getOrDefault("TOURISM_TAX", "4.50"));
        ReportCatalog reports = new ReportCatalog(reservations, rooms, tourismTax);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        new Api(service, rooms, messages, reports).build().start(port);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        }
        return value;
    }
}
