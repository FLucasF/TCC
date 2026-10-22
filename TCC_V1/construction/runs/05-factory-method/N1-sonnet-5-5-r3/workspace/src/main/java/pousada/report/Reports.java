package pousada.report;

import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.domain.Rooms;

import java.math.BigDecimal;
import java.util.List;

/** Relatórios em CSV; recebem as reservas candidatas já ordenadas e escolhem as que contam. */
public final class Reports {

    private Reports() {
    }

    /** Ocupação de uma noite: reservas pagas, em ordem de quarto (as candidatas já ocupam a noite). */
    public static String occupancy(List<Reservation> occupyingNight) {
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        int occupied = 0;
        for (Reservation r : occupyingNight) {
            if (!r.status().isPaid()) {
                continue;
            }
            occupied++;
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id())
                    .append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        return csv.append("total,").append(occupied).append('/').append(Rooms.all().size()).append('\n').toString();
    }

    /** Financeiro de um mês: reservas que deixaram dinheiro (pago menos devolvido) com a pousada. */
    public static String revenue(List<Reservation> startingInMonth) {
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : startingInMonth) {
            if (r.retained().signum() <= 0) {
                continue;
            }
            sum = sum.add(r.retained());
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn())
                    .append(',').append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.retained().toPlainString()).append('\n');
        }
        return csv.append("total,").append(sum.toPlainString()).append('\n').toString();
    }
}
