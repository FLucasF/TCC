package br.com.marealta;

import br.com.marealta.http.App;
import br.com.marealta.persistence.Database;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;

public class Main {
    public static void main(String[] args) {
        Database database = new Database(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        database.createSchema();

        String port = System.getenv("PORT");
        new App(new ReservationService(database), new ReportService(database))
                .create()
                .start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
