package br.com.pousadamarealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public final class Reports {
    private Reports() {}

    public static String occupancy(List<Reservation> all, LocalDate night) {
        List<Reservation> rows = all.stream()
                .filter(r -> r.paid() && !night.isBefore(r.checkIn) && night.isBefore(r.checkOut))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room.number()).thenComparingLong(r -> r.id))
                .toList();
        StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : rows) {
            sb.append(r.room.number()).append(',').append(r.room.type()).append(',').append(r.id).append(',')
                    .append(r.checkIn).append(',').append(r.checkOut).append('\n');
        }
        long occupied = rows.stream().map(r -> r.room.number()).distinct().count();
        sb.append("total,").append(occupied).append('/').append(Room.ALL.size()).append('\n');
        return sb.toString();
    }

    public static String revenue(List<Reservation> all, YearMonth month) {
        List<Reservation> rows = all.stream()
                .filter(r -> YearMonth.from(r.checkIn).equals(month) && r.netRevenue().signum() > 0)
                .sorted(Comparator.comparing((Reservation r) -> r.checkIn).thenComparingLong(r -> r.id))
                .toList();
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : rows) {
            sb.append(r.id).append(',').append(r.room.number()).append(',').append(r.checkIn).append(',')
                    .append(r.checkOut).append(',').append(r.status).append(',')
                    .append(r.netRevenue().toPlainString()).append('\n');
            sum = Money.round(sum.add(r.netRevenue()));
        }
        sb.append("total,").append(sum.toPlainString()).append('\n');
        return sb.toString();
    }
}
