package pousada;

import pousada.api.Api;
import pousada.infra.Database;
import pousada.infra.PostgresReservationRepository;
import pousada.report.Reports;
import pousada.service.ReservationRepository;
import pousada.service.ReservationService;

import javax.sql.DataSource;

public class Main {
    public static void main(String[] args) {
        DataSource dataSource = Database.connect(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        ReservationRepository repository = new PostgresReservationRepository(dataSource);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        new Api(new ReservationService(repository), new Reports(repository)).create().start(port);
    }
}
