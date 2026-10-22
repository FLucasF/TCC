package br.com.marealta.domain;

import java.math.BigDecimal;
import java.util.List;

public final class Reports {
    private Reports() {
    }

    /** Reservas pagas que ocupam o quarto na noite, já em ordem de quarto. */
    public static String occupancy(List<Reservation> occupying) {
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',')
                    .append(r.id()).append(',').append(r.checkInDate()).append(',')
                    .append(r.checkOutDate()).append('\n');
        }
        csv.append("total,").append(occupying.size()).append('/').append(Rooms.all().size()).append('\n');
        return csv.toString();
    }

    /** Reservas do mês já em ordem; entram só as que deixaram dinheiro com a pousada. */
    public static String revenue(List<Reservation> startingInMonth) {
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.round(BigDecimal.ZERO);
        for (Reservation r : startingInMonth) {
            BigDecimal amount = r.retainedAmount();
            if (amount.signum() <= 0) {
                continue;
            }
            total = total.add(amount);
            csv.append(r.id()).append(',').append(r.room().number()).append(',')
                    .append(r.checkInDate()).append(',').append(r.checkOutDate()).append(',')
                    .append(r.status()).append(',').append(amount.toPlainString()).append('\n');
        }
        csv.append("total,").append(total.toPlainString()).append('\n');
        return csv.toString();
    }
}
