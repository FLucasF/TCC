package br.com.pousadamarealta.reservas.report;

import br.com.pousadamarealta.reservas.domain.Money;
import br.com.pousadamarealta.reservas.domain.Reservation;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public final class RevenueReport {

    private RevenueReport() {
    }

    public static String render(List<Reservation> reservations, YearMonth month) {
        List<Reservation> kept = reservations.stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month) && r.amountKept().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
        List<String> rows = kept.stream()
                .map(r -> String.join(",", String.valueOf(r.id()), String.valueOf(r.room().number()),
                        r.checkIn().toString(), r.checkOut().toString(), r.status().name(),
                        r.amountKept().toPlainString()))
                .toList();
        BigDecimal total = kept.stream().map(Reservation::amountKept).reduce(Money.ZERO, BigDecimal::add);
        return CsvReport.render("reservation,room,check_in,check_out,status,amount", rows, total.toPlainString());
    }
}
