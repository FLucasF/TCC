package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

final class Reports {
    private Reports() {}

    static String occupancy(List<Reservation> all, LocalDate night) {
        List<Reservation> rows = all.stream()
                .filter(r -> r.status.isPaid() && !night.isBefore(r.checkIn) && night.isBefore(r.checkOut))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room.number()).thenComparingInt(r -> r.id))
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

    static String revenue(List<Reservation> all, YearMonth month) {
        List<Reservation> rows = all.stream()
                .filter(r -> YearMonth.from(r.checkIn).equals(month) && r.netAmount().signum() > 0)
                .sorted(Comparator.comparing((Reservation r) -> r.checkIn).thenComparingInt(r -> r.id))
                .toList();
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : rows) {
            sb.append(r.id).append(',').append(r.room.number()).append(',').append(r.checkIn).append(',')
                    .append(r.checkOut).append(',').append(r.status).append(',')
                    .append(Money.plain(r.netAmount())).append('\n');
            sum = sum.add(r.netAmount());
        }
        sb.append("total,").append(Money.plain(sum)).append('\n');
        return sb.toString();
    }
}
