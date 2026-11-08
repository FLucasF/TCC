package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportService {
    private final Transactions tx;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public ReportService(Transactions tx, RoomRepository rooms, ReservationRepository reservations) {
        this.tx = tx;
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        return tx.run(() -> {
            Map<Integer, Room> byNumber = rooms.findAll().stream()
                    .collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = reservations.findPaidOccupying(night);
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                csv.append(r.roomNumber()).append(',')
                        .append(byNumber.get(r.roomNumber()).type()).append(',')
                        .append(r.id()).append(',')
                        .append(r.checkIn()).append(',')
                        .append(r.checkOut()).append('\n');
            }
            long occupied = occupying.stream().map(Reservation::roomNumber).distinct().count();
            csv.append("total,").append(occupied).append('/').append(byNumber.size()).append('\n');
            return csv.toString();
        });
    }

    public String revenue(YearMonth month) {
        return tx.run(() -> {
            List<Reservation> starting = reservations.findCheckingInBetween(
                    month.atDay(1), month.plusMonths(1).atDay(1));
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal total = Money.ZERO;
            for (Reservation r : starting) {
                BigDecimal amount = r.netAmount();
                if (amount.signum() <= 0) {
                    continue;
                }
                csv.append(r.id()).append(',')
                        .append(r.roomNumber()).append(',')
                        .append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',')
                        .append(r.status()).append(',')
                        .append(amount.toPlainString()).append('\n');
                total = total.add(amount);
            }
            csv.append("total,").append(Money.round(total).toPlainString()).append('\n');
            return csv.toString();
        });
    }
}
