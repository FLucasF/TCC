package br.com.marealta.report;

import br.com.marealta.db.ReservationRepository;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class RevenueReport implements Report {

    private final ReservationRepository reservations;

    public RevenueReport(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override public String name() { return "revenue"; }

    @Override public PeriodKind periodKind() { return PeriodKind.MONTH; }

    @Override
    public ReportTable build(Connection c, String period) throws SQLException {
        List<Reservation> list = MonthlyReservations.withRevenue(reservations, c, period);
        BigDecimal total = Money.ZERO;
        List<List<String>> rows = new java.util.ArrayList<>();
        for (Reservation r : list) {
            total = total.add(r.amountKept());
            rows.add(List.of(String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                    r.checkOut().toString(), r.status().name(), ReportRenderer.money(r.amountKept())));
        }
        return new ReportTable(name(), period,
                List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", ReportRenderer.money(total)));
    }
}
