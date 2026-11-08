package br.com.pousadamarealta.reservas.service;

import br.com.pousadamarealta.reservas.domain.Money;
import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.Room;
import br.com.pousadamarealta.reservas.persistence.Database;
import br.com.pousadamarealta.reservas.persistence.ReservationRepository;
import br.com.pousadamarealta.reservas.persistence.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportService {
    private final Database db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public ReportService(Database db, RoomRepository rooms, ReservationRepository reservations) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        return db.transaction(() -> {
            List<Room> allRooms = rooms.findAll();
            Map<Integer, Room> byNumber = allRooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = reservations.findPaidOccupying(night);
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                csv.append(r.roomNumber()).append(',')
                        .append(byNumber.get(r.roomNumber()).type()).append(',')
                        .append(r.id()).append(',')
                        .append(r.checkIn()).append(',')
                        .append(r.checkOut()).append('\n');
            }
            csv.append("total,").append(occupying.size()).append('/').append(allRooms.size()).append('\n');
            return csv.toString();
        });
    }

    public String revenue(YearMonth month) {
        return db.transaction(() -> {
            List<Reservation> list = reservations.findWithRevenueStartingBetween(
                    month.atDay(1), month.plusMonths(1).atDay(1));
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Money.ZERO;
            for (Reservation r : list) {
                BigDecimal kept = Money.round(r.amountPaid().subtract(r.refund() == null ? Money.ZERO : r.refund()));
                sum = sum.add(kept);
                csv.append(r.id()).append(',')
                        .append(r.roomNumber()).append(',')
                        .append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',')
                        .append(r.status()).append(',')
                        .append(kept.toPlainString()).append('\n');
            }
            csv.append("total,").append(Money.round(sum).toPlainString()).append('\n');
            return csv.toString();
        });
    }
}
