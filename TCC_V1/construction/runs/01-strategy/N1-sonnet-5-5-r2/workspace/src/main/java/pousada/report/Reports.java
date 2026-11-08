package pousada.report;

import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.domain.RoomCatalog;
import pousada.service.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class Reports {
    private final ReservationRepository repository;

    public Reports(ReservationRepository repository) {
        this.repository = repository;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = repository.paidOnNight(night).stream()
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()).thenComparingLong(Reservation::id))
                .toList();
        List<String> rows = occupying.stream()
                .map(r -> String.join(",", String.valueOf(r.room().number()), r.room().type().name(),
                        String.valueOf(r.id()), r.checkInDate().toString(), r.checkOutDate().toString()))
                .toList();
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return Csv.render("room,type,reservation,check_in,check_out", rows,
                occupiedRooms + "/" + RoomCatalog.all().size());
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = repository.checkInBetween(month.atDay(1), month.atEndOfMonth()).stream()
                .filter(r -> r.netAmount().signum() > 0)
                .sorted(Comparator.comparing(Reservation::checkInDate).thenComparingLong(Reservation::id))
                .toList();
        List<String> rows = kept.stream()
                .map(r -> String.join(",", String.valueOf(r.id()), String.valueOf(r.room().number()),
                        r.checkInDate().toString(), r.checkOutDate().toString(), r.status().name(),
                        r.netAmount().toPlainString()))
                .toList();
        BigDecimal sum = kept.stream().map(Reservation::netAmount).reduce(Money.ZERO, BigDecimal::add);
        return Csv.render("reservation,room,check_in,check_out,status,amount", rows, sum.toPlainString());
    }
}
