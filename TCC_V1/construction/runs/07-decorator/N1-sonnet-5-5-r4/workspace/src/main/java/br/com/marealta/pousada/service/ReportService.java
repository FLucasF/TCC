package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Rooms;
import br.com.marealta.pousada.persistence.Database;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class ReportService {
    private final Database database;

    public ReportService(Database database) {
        this.database = database;
    }

    public Report occupancy(LocalDate night) {
        List<Reservation> occupying = database.transaction(tx -> tx.reservations().findPaidOccupying(night));
        List<List<String>> rows = occupying.stream()
                .map(r -> List.of(String.valueOf(r.room().number()), r.room().type().name(), String.valueOf(r.id()),
                        r.checkIn().toString(), r.checkOut().toString()))
                .toList();
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return new Report(List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", occupiedRooms + "/" + Rooms.ALL.size()));
    }

    public Report revenue(YearMonth month) {
        List<Reservation> kept = database.transaction(tx ->
                tx.reservations().findKeepingMoneyStartingBetween(month.atDay(1), month.atEndOfMonth()));
        List<List<String>> rows = kept.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.room().number()), r.checkIn().toString(),
                        r.checkOut().toString(), r.status().name(), r.kept().toPlainString()))
                .toList();
        BigDecimal sum = kept.stream().map(Reservation::kept).reduce(Money.ZERO, BigDecimal::add);
        return new Report(List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", sum.toPlainString()));
    }
}
