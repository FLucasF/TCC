package br.com.marealta.pousada.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;

public final class RevenueReport {

    private RevenueReport() {
    }

    public static String render(YearMonth month, List<Reservation> reservations) {
        List<Reservation> kept = reservations.stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month))
                .filter(r -> r.retainedAmount().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.retainedAmount().toPlainString()).append('\n');
            sum = sum.add(r.retainedAmount());
        }
        return csv.append("total,").append(sum.toPlainString()).append('\n').toString();
    }
}
