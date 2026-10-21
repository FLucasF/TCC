package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public final class Reports {
    private Reports() {
    }

    public static String occupancy(List<Reservation> reservations, LocalDate night) {
        List<Reservation> occupying = reservations.stream()
                .filter(r -> r.status().isPaid() && r.occupies(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()).thenComparingInt(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long rooms = occupying.stream().map(Reservation::room).distinct().count();
        csv.append("total,").append(rooms).append('/').append(RoomCatalog.all().size()).append('\n');
        return csv.toString();
    }

    public static String revenue(List<Reservation> reservations, YearMonth month) {
        List<Reservation> kept = reservations.stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month) && r.retainedAmount().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append(',')
                    .append(r.status()).append(',').append(r.retainedAmount()).append('\n');
            total = total.add(r.retainedAmount());
        }
        csv.append("total,").append(total).append('\n');
        return csv.toString();
    }
}
