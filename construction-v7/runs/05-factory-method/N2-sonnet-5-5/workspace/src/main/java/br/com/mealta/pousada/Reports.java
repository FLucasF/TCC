package br.com.mealta.pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public final class Reports {
    private Reports() {}

    public static String occupancy(LocalDate night, List<Reservation> all, List<Room> rooms) {
        List<Reservation> occupying = all.stream()
                .filter(r -> r.occupiesNight(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()).thenComparingLong(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id())
                    .append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return csv.append("total,").append(occupiedRooms).append('/').append(rooms.size()).append('\n').toString();
    }

    public static String revenue(YearMonth month, List<Reservation> all) {
        List<Reservation> kept = all.stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month) && r.retained().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.round(BigDecimal.ZERO);
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn())
                    .append(',').append(r.checkOut()).append(',').append(r.status())
                    .append(',').append(r.retained().toPlainString()).append('\n');
            total = total.add(r.retained());
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
