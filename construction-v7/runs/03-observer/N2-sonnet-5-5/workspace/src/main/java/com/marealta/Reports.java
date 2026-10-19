package com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class Reports {
    private final ReservationService service;

    public Reports(ReservationService service) { this.service = service; }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = service.snapshot().stream()
                .filter(r -> r.status().isPaid())
                .filter(r -> !night.isBefore(r.checkIn()) && night.isBefore(r.checkOut()))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        csv.append("total,").append(occupying.size()).append('/').append(service.rooms().size()).append('\n');
        return csv.toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> rows = service.snapshot().stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month))
                .filter(r -> r.retainedAmount().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id))
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : rows) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.retainedAmount().toPlainString()).append('\n');
            sum = sum.add(r.retainedAmount());
        }
        csv.append("total,").append(Money.round(sum).toPlainString()).append('\n');
        return csv.toString();
    }
}
