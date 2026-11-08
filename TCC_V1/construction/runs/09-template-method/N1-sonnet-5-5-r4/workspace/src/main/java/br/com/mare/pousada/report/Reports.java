package br.com.mare.pousada.report;

import br.com.mare.pousada.infra.ReservationRepository;

import java.util.List;

/** Os relatórios que o sistema oferece. Um relatório novo entra aqui e ganha o endereço /reports/{name}. */
public final class Reports {
    private Reports() {
    }

    public static List<Report<?>> all(ReservationRepository reservations) {
        return List.of(
                new OccupancyReport(reservations),
                new RevenueReport(reservations),
                new TourismTaxReport(reservations),
                new RatePlanReport(reservations));
    }
}
