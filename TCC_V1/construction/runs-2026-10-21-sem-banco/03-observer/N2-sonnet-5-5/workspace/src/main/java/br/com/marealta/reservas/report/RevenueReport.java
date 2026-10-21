package br.com.marealta.reservas.report;

import br.com.marealta.reservas.domain.Money;
import br.com.marealta.reservas.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class RevenueReport {

    public String generate(List<Reservation> reservations, YearMonth month) {
        List<Reservation> kept = reservations.stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month))
                .filter(r -> r.retained().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn())
                    .append(',').append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(Csv.money(r.retained())).append('\n');
            total = total.add(r.retained());
        }
        return csv.append("total,").append(Csv.money(total)).append('\n').toString();
    }
}
