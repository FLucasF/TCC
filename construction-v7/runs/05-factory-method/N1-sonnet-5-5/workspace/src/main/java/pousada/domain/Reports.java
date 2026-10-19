package pousada.domain;

import pousada.domain.Room.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** Relatórios em CSV (uma linha por registro, "\n" no fim de cada). */
public final class Reports {

    private Reports() {
    }

    public static String occupancy(LocalDate night, List<Reservation> reservations, List<Room> rooms) {
        List<Reservation> occupying = reservations.stream()
                .filter(r -> r.status().isPaid() && r.occupiesNight(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            RoomType type = r.room().type();
            csv.append(r.room().number()).append(',').append(type).append(',').append(r.id()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long occupied = occupying.stream().map(r -> r.room().number()).distinct().count();
        return csv.append("total,").append(occupied).append('/').append(rooms.size()).append('\n').toString();
    }

    public static String revenue(YearMonth month, List<Reservation> reservations) {
        List<Reservation> kept = reservations.stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month) && r.kept().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.round(BigDecimal.ZERO);
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.kept().toPlainString()).append('\n');
            total = total.add(r.kept());
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
