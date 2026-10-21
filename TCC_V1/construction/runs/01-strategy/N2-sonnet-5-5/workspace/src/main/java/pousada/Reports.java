package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class Reports {
    private final Hotel hotel;

    public Reports(Hotel hotel) {
        this.hotel = hotel;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = hotel.reservations().stream()
                .filter(r -> r.status().isPaid() && !night.isBefore(r.checkIn) && night.isBefore(r.checkOut))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room.number()))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room.number()).append(',').append(r.room.type()).append(',').append(r.id).append(',')
                    .append(r.checkIn).append(',').append(r.checkOut).append('\n');
        }
        long occupiedRooms = occupying.stream().map(r -> r.room).distinct().count();
        return csv.append("total,").append(occupiedRooms).append('/').append(hotel.rooms().size()).append('\n').toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = hotel.reservations().stream()
                .filter(r -> YearMonth.from(r.checkIn).equals(month) && r.netAmount().signum() > 0)
                .sorted(Comparator.comparing((Reservation r) -> r.checkIn).thenComparingInt(r -> r.id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(r.id).append(',').append(r.room.number()).append(',').append(r.checkIn).append(',')
                    .append(r.checkOut).append(',').append(r.status()).append(',').append(r.netAmount()).append('\n');
            total = total.add(r.netAmount());
        }
        return csv.append("total,").append(total).append('\n').toString();
    }
}
