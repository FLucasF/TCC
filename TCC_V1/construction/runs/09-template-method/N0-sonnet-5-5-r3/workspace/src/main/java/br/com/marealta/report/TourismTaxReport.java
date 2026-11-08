package br.com.marealta.report;

import br.com.marealta.db.ReservationRepository;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Tourism tax charged by the city: a fixed amount per guest per night, on paid reservations. */
public final class TourismTaxReport implements Report {

    private final ReservationRepository reservations;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationRepository reservations, BigDecimal taxPerGuestNight) {
        this.reservations = reservations;
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override public String name() { return "tourism-tax"; }

    @Override public PeriodKind periodKind() { return PeriodKind.MONTH; }

    @Override
    public ReportTable build(Connection c, String period) throws SQLException {
        BigDecimal total = Money.ZERO;
        List<List<String>> rows = new ArrayList<>();
        for (Reservation r : MonthlyReservations.starting(reservations, c, period)) {
            if (!r.isPaid()) continue;
            BigDecimal tax = Money.round(taxPerGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
            total = total.add(tax);
            rows.add(List.of(String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                    r.checkOut().toString(), String.valueOf(r.guests()), String.valueOf(r.nights()),
                    ReportRenderer.money(tax)));
        }
        return new ReportTable(name(), period,
                List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"), rows,
                List.of("total", ReportRenderer.money(total)));
    }
}
