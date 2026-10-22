package com.marealta.pousada.domain;

import com.marealta.pousada.persistence.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public final class ReportService {
    private final ReservationRepository reservations;
    private final RoomCatalog rooms;

    public ReportService(ReservationRepository reservations, RoomCatalog rooms) {
        this.reservations = reservations;
        this.rooms = rooms;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = reservations.findOverlapping(night, night.plusDays(1)).stream()
                .filter(r -> r.status().isPaid() && r.occupiesNight(night))
                .sorted(Comparator.comparingInt(Reservation::roomNumber))
                .toList();
        List<List<String>> rows = occupying.stream().map(r -> List.of(
                String.valueOf(r.roomNumber()), rooms.find(r.roomNumber()).orElseThrow().type().name(),
                String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString())).toList();
        return Csv.render(List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", occupying.size() + "/" + rooms.all().size()));
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = reservations.findByCheckInBetween(month.atDay(1), month.atEndOfMonth()).stream()
                .filter(r -> r.kept().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
        List<List<String>> rows = kept.stream().map(r -> List.of(
                String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                r.checkOut().toString(), r.status().name(), r.kept().toPlainString())).toList();
        BigDecimal total = kept.stream().map(Reservation::kept).reduce(Money.ZERO, BigDecimal::add);
        return Csv.render(List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", total.toPlainString()));
    }
}
