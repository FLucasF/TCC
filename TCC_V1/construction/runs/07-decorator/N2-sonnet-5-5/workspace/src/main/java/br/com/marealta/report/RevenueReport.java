package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class RevenueReport {
    private RevenueReport() {}

    /** Recebe as reservas que começam no mês; entram as que deixaram dinheiro com a pousada. */
    public static String render(List<Reservation> startingInMonth) {
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        List<Reservation> kept = startingInMonth.stream()
                .filter(r -> r.amountKept().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id))
                .toList();
        for (Reservation r : kept) {
            csv.append(r.id()).append(',')
                    .append(r.roomNumber()).append(',')
                    .append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',')
                    .append(r.status()).append(',')
                    .append(r.amountKept().toPlainString()).append('\n');
            total = Money.round(total.add(r.amountKept()));
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
