package br.com.marealta.service;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.Rooms;
import br.com.marealta.infra.Database;
import br.com.marealta.infra.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private final Database db;
    private final ReservationRepository reservations;

    public ReportService(Database db, ReservationRepository reservations) {
        this.db = db;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = db.inTransaction(c -> reservations.findOverlapping(c, night, night.plusDays(1)))
                .stream()
                .filter(r -> r.status().isPaid())
                .sorted(Comparator.comparingInt(Reservation::roomNumber))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            Room room = Rooms.find(r.roomNumber()).orElseThrow();
            csv.append(room.number()).append(',').append(room.type()).append(',').append(r.id()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        return csv.append("total,").append(occupying.size()).append('/').append(Rooms.ALL.size()).append('\n')
                .toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = db.inTransaction(
                        c -> reservations.findStartingBetween(c, month.atDay(1), month.plusMonths(1).atDay(1)))
                .stream()
                .filter(r -> kept(r).signum() > 0)
                .toList();
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        for (Reservation r : kept) {
            BigDecimal amount = kept(r);
            sum = sum.add(amount);
            csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',').append(amount.toPlainString())
                    .append('\n');
        }
        return csv.append("total,").append(sum.toPlainString()).append('\n').toString();
    }

    /** Dinheiro que fica com a pousada: o que foi pago menos o que foi devolvido. */
    private static BigDecimal kept(Reservation r) {
        return r.refund() == null ? r.amountPaid() : r.amountPaid().subtract(r.refund());
    }
}
