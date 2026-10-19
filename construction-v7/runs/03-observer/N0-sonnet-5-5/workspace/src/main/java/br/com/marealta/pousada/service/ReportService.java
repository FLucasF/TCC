package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private final ReservationService reservations;

    public ReportService(ReservationService reservations) {
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> rows = reservations.all().stream()
                .filter(r -> r.status().paid() && r.occupies(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number())
                        .thenComparingLong(Reservation::id))
                .toList();
        StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : rows) {
            sb.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id())
                    .append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long occupied = rows.stream().map(r -> r.room().number()).distinct().count();
        sb.append("total,").append(occupied).append('/').append(reservations.rooms().size()).append('\n');
        return sb.toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> rows = reservations.all().stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month) && r.netRevenue().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : rows) {
            sb.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.netRevenue().toPlainString()).append('\n');
            total = total.add(r.netRevenue());
        }
        sb.append("total,").append(Money.round(total).toPlainString()).append('\n');
        return sb.toString();
    }
}
