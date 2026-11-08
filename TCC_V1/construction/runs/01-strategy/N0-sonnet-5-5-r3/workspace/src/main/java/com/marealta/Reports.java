package com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Reports {
    private Reports() {}

    public static String occupancy(List<Room> rooms, List<Reservation> covering) {
        Map<Integer, Room> byNumber = rooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
        StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
        int occupied = 0;
        for (Reservation r : covering) {
            if (!r.status().isPaid()) continue;
            occupied++;
            sb.append(r.roomNumber()).append(',').append(byNumber.get(r.roomNumber()).type()).append(',')
                    .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        return sb.append("total,").append(occupied).append('/').append(rooms.size()).append('\n').toString();
    }

    /** Dinheiro que fica com a pousada: pago menos devolvido. */
    public static BigDecimal kept(Reservation r) {
        return Money.round(r.amountPaid().subtract(r.refund() == null ? Money.ZERO : r.refund()));
    }

    public static String revenue(List<Reservation> starting) {
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : starting) {
            BigDecimal kept = kept(r);
            if (kept.signum() <= 0) continue;
            total = total.add(kept);
            sb.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',').append(kept).append('\n');
        }
        return sb.append("total,").append(Money.round(total)).append('\n').toString();
    }

}
