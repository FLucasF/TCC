package marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class Reports {
    private final ReservationService service;

    public Reports(ReservationService service) {
        this.service = service;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = service.reservations().stream()
                .filter(r -> r.isPaid() && r.occupiesNight(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',')
                    .append(r.id()).append(',').append(r.checkInDate()).append(',')
                    .append(r.checkOutDate()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(Reservation::room).distinct().count();
        csv.append("total,").append(occupiedRooms).append('/').append(service.rooms().size()).append('\n');
        return csv.toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = service.reservations().stream()
                .filter(r -> YearMonth.from(r.checkInDate()).equals(month) && r.netAmount().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkInDate).thenComparingInt(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',')
                    .append(r.checkInDate()).append(',').append(r.checkOutDate()).append(',')
                    .append(r.status()).append(',').append(r.netAmount().toPlainString()).append('\n');
            total = total.add(r.netAmount());
        }
        csv.append("total,").append(total.toPlainString()).append('\n');
        return csv.toString();
    }
}
