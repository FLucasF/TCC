package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private final RoomCatalog rooms;
    private final ReservationRepository repository;

    public ReportService(RoomCatalog rooms, ReservationRepository repository) {
        this.rooms = rooms;
        this.repository = repository;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying;
        synchronized (repository) {
            occupying = repository.all().stream()
                    .filter(r -> r.occupiesNight(night))
                    .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()))
                    .toList();
        }
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',')
                    .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        return csv.append("total,").append(occupying.size()).append('/').append(rooms.all().size())
                .append('\n').toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept;
        synchronized (repository) {
            kept = repository.all().stream()
                    .filter(r -> YearMonth.from(r.checkIn()).equals(month))
                    .filter(r -> r.amountKept().signum() > 0)
                    .sorted(Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id))
                    .toList();
        }
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append(',')
                    .append(r.status()).append(',').append(r.amountKept().toPlainString()).append('\n');
            total = total.add(r.amountKept());
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
