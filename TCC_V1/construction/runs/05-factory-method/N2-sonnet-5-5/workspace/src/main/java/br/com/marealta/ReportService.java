package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservations;
    private final RoomCatalog rooms;

    public ReportService(ReservationRepository reservations, RoomCatalog rooms) {
        this.reservations = reservations;
        this.rooms = rooms;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = reservations.findAll().stream()
                .filter(r -> r.status().paid())
                .filter(r -> r.occupies(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()).thenComparing(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id())
                    .append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long rooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return csv.append("total,").append(rooms).append('/').append(this.rooms.count()).append('\n').toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = reservations.findAll().stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month))
                .filter(r -> r.retained().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.zero();
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.retained().toPlainString()).append('\n');
            sum = sum.add(r.retained());
        }
        return csv.append("total,").append(sum.toPlainString()).append('\n').toString();
    }
}
