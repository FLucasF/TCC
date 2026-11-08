package br.com.mare.pousada.report;

import br.com.mare.pousada.domain.Money;
import br.com.mare.pousada.domain.Reservation;
import br.com.mare.pousada.domain.TourismTax;
import br.com.mare.pousada.infra.ReservationRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

public class TourismTaxReport extends MonthlyReport {
    private final ReservationRepository reservations;

    public TourismTaxReport(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    protected Table build(Connection connection, YearMonth month) throws SQLException {
        List<Reservation> paid = reservations.findStartingIn(connection, month).stream()
                .filter(r -> r.getStatus().isPaid())
                .toList();
        BigDecimal sum = paid.stream().map(TourismTax::of).reduce(Money.ZERO, BigDecimal::add);
        List<List<String>> rows = paid.stream()
                .map(r -> List.of(String.valueOf(r.getId()), String.valueOf(r.getRoomNumber()),
                        r.getCheckIn().toString(), r.getCheckOut().toString(), String.valueOf(r.getGuests()),
                        String.valueOf(r.getNights()), Money.plain(TourismTax.of(r))))
                .toList();
        return new Table(List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"), rows,
                List.of("total", Money.plain(sum)));
    }
}
