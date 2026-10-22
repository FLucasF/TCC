package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.persistence.Db;
import br.com.marealta.persistence.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public final class RevenueReport {
    private static final List<String> HEADER =
            List.of("reservation", "room", "check_in", "check_out", "status", "amount");

    private final Db db;
    private final ReservationRepository reservations;

    public RevenueReport(Db db, ReservationRepository reservations) {
        this.db = db;
        this.reservations = reservations;
    }

    public String generate(YearMonth month) {
        return db.transaction(connection -> {
            List<Reservation> kept = reservations
                    .findStartingBetween(connection, month.atDay(1), month.plusMonths(1).atDay(1)).stream()
                    .filter(r -> r.kept().signum() > 0)
                    .toList();
            List<List<String>> rows = kept.stream()
                    .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.roomNumber()),
                            r.arrival().toString(), r.departure().toString(), r.status().name(),
                            r.kept().toPlainString()))
                    .toList();
            BigDecimal total = kept.stream().map(Reservation::kept).reduce(Money.ZERO, BigDecimal::add);
            return Csv.render(HEADER, rows, Money.round(total).toPlainString());
        });
    }
}
