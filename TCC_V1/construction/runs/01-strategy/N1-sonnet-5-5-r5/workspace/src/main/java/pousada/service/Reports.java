package pousada.service;

import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.domain.Rooms;

import java.math.BigDecimal;
import java.util.List;

/** Os relatórios em CSV. Recebem as reservas já filtradas e ordenadas. */
public final class Reports {
    private Reports() {
    }

    public static BigDecimal kept(Reservation r) {
        return r.amountPaid().subtract(r.refund() == null ? Money.ZERO : r.refund());
    }

    public static String occupancy(List<Reservation> occupying) {
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.roomNumber()).append(',')
                    .append(Rooms.find(r.roomNumber()).orElseThrow().type()).append(',')
                    .append(r.id()).append(',')
                    .append(r.checkIn()).append(',')
                    .append(r.checkOut()).append('\n');
        }
        return csv.append("total,").append(occupying.size()).append('/').append(Rooms.count()).append('\n')
                .toString();
    }

    public static String revenue(List<Reservation> withRevenue) {
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : withRevenue) {
            BigDecimal amount = kept(r);
            total = total.add(amount);
            csv.append(r.id()).append(',')
                    .append(r.roomNumber()).append(',')
                    .append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',')
                    .append(r.status()).append(',')
                    .append(amount.toPlainString()).append('\n');
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
