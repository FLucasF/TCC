package br.com.marealta;

import br.com.marealta.report.ReportService;
import br.com.marealta.report.TourismTax;
import br.com.marealta.service.ReservationService;
import br.com.marealta.store.PostgresStore;
import br.com.marealta.web.App;

import java.math.BigDecimal;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        String url = required("DB_URL");
        PostgresStore store = new PostgresStore(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        store.initialize();

        // The city changes the tax every year: set TOURISM_TAX_PER_GUEST_NIGHT instead of changing code.
        BigDecimal tax = new BigDecimal(env("TOURISM_TAX_PER_GUEST_NIGHT", "4.50"));
        ReportService reports = new ReportService(store, ReportService.standardReports(TourismTax.flat(tax)));
        int port = Integer.parseInt(env("PORT", "7070"));
        App.create(new ReservationService(store), reports).start(port);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is required");
        }
        return value;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
