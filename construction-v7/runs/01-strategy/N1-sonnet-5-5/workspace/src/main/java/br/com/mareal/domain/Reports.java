package br.com.mareal.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** CSV reports. Lines end with \n. */
public final class Reports {
    private Reports() {}

    public static String occupancy(LocalDate night, List<Reservation> all, List<Room> rooms) {
        List<Reservation> occupying = all.stream()
                .filter(r -> r.status.isPaid() && r.occupies(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room.number()).thenComparingInt(r -> r.id))
                .toList();
        StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            sb.append(r.room.number()).append(',').append(r.room.type()).append(',').append(r.id).append(',')
                    .append(r.checkIn).append(',').append(r.checkOut).append('\n');
        }
        long occupiedRooms = occupying.stream().map(r -> r.room.number()).distinct().count();
        sb.append("total,").append(occupiedRooms).append('/').append(rooms.size()).append('\n');
        return sb.toString();
    }

    public static String revenue(YearMonth month, List<Reservation> all) {
        List<Reservation> kept = all.stream()
                .filter(r -> YearMonth.from(r.checkIn).equals(month) && r.netAmount().signum() > 0)
                .sorted(Comparator.comparing((Reservation r) -> r.checkIn).thenComparingInt(r -> r.id))
                .toList();
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : kept) {
            sb.append(r.id).append(',').append(r.room.number()).append(',').append(r.checkIn).append(',')
                    .append(r.checkOut).append(',').append(r.status).append(',').append(r.netAmount()).append('\n');
            sum = sum.add(r.netAmount());
        }
        sb.append("total,").append(Money.round(sum)).append('\n');
        return sb.toString();
    }
}
