package br.com.marealta.report;

import br.com.marealta.app.ReservationRepository;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class RevenueReport {
    private final ReservationRepository reservations;

    public RevenueReport(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    public String generate(YearMonth month) {
        List<Reservation> kept = reservations.findAll().stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month) && r.kept().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id))
                .toList();
        BigDecimal sum = kept.stream().map(Reservation::kept).reduce(Money.ZERO, BigDecimal::add);
        return Csv.build(
                List.of("reservation", "room", "check_in", "check_out", "status", "amount"),
                kept,
                r -> List.of(r.id(), r.roomNumber(), r.checkIn(), r.checkOut(), r.status(), r.kept()),
                sum.toPlainString());
    }
}
