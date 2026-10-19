package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import static pousada.Money.round;

public class Reports {
    private final ReservationService service;

    public Reports(ReservationService service) {
        this.service = service;
    }

    public String occupancy(LocalDate night) {
        return service.read(all -> {
            List<Reservation> occupying = all.stream()
                    .filter(r -> r.status().isPaid() && r.occupies(night))
                    .sorted(Comparator.comparingInt(r -> r.room.number()))
                    .toList();
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                csv.append(r.room.number()).append(',').append(r.room.type()).append(',').append(r.id).append(',')
                        .append(r.checkIn).append(',').append(r.checkOut).append('\n');
            }
            return csv.append("total,").append(occupying.size()).append('/').append(ReservationService.ROOMS.size())
                    .append('\n').toString();
        });
    }

    public String revenue(YearMonth month) {
        return service.read(all -> {
            List<Reservation> kept = all.stream()
                    .filter(r -> YearMonth.from(r.checkIn).equals(month) && r.retained().signum() > 0)
                    .sorted(Comparator.comparing((Reservation r) -> r.checkIn).thenComparingInt(r -> r.id))
                    .toList();
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = round(BigDecimal.ZERO);
            for (Reservation r : kept) {
                csv.append(r.id).append(',').append(r.room.number()).append(',').append(r.checkIn).append(',')
                        .append(r.checkOut).append(',').append(r.status()).append(',').append(r.retained()).append('\n');
                sum = sum.add(r.retained());
            }
            return csv.append("total,").append(round(sum)).append('\n').toString();
        });
    }
}
