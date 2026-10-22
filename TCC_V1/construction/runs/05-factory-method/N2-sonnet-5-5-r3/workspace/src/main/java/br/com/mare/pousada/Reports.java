package br.com.mare.pousada;

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
        List<Reservation> occupying = service.all().stream()
                .filter(r -> r.status().isPaid() && r.occupies(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',')
                    .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long rooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return csv.append("total,").append(rooms).append('/').append(RoomCatalog.all().size()).append('\n')
                .toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = service.all().stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month) && r.amountKept().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.amountKept().toPlainString()).append('\n');
            total = Money.round(total.add(r.amountKept()));
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
