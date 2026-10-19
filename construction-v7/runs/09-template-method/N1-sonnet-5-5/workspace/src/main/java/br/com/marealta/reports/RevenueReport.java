package br.com.marealta.reports;

import br.com.marealta.Money;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport extends MonthReport {

    public RevenueReport(ReservationService reservations) {
        super("revenue", reservations);
    }

    @Override
    Table table(YearMonth month) {
        List<Reservation> kept = retainingMoneyIn(month);
        List<List<String>> rows = kept.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.room().number()),
                        r.checkIn().toString(), r.checkOut().toString(), r.status().name(),
                        Money.csv(r.retained())))
                .toList();
        BigDecimal total = kept.stream().map(Reservation::retained)
                .reduce(Money.round(BigDecimal.ZERO), BigDecimal::add);
        return new Table(List.of("reservation", "room", "check_in", "check_out", "status", "amount"),
                rows, List.of("total", Money.csv(total)));
    }
}
