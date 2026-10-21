package br.com.marealta.report;

import br.com.marealta.domain.Formats;
import br.com.marealta.domain.Reservation;
import br.com.marealta.service.ReservationService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class RevenueReport {
    private final ReservationService reservations;

    public RevenueReport(ReservationService reservations) {
        this.reservations = reservations;
    }

    public String generate(YearMonth month) {
        List<Reservation> kept = reservations.query(all -> all.stream()
                .filter(r -> YearMonth.from(r.checkInDate()).equals(month) && r.amountKept().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkInDate).thenComparingLong(Reservation::id))
                .toList());
        List<List<String>> rows = kept.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.room().number()),
                        r.checkInDate().toString(), r.checkOutDate().toString(), r.status().name(),
                        r.amountKept().toPlainString()))
                .toList();
        BigDecimal sum = kept.stream().map(Reservation::amountKept)
                .reduce(Formats.round(BigDecimal.ZERO), BigDecimal::add);
        return Csv.render(List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", sum.toPlainString()));
    }
}
