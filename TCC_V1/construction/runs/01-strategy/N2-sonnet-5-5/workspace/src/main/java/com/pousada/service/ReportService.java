package com.pousada.service;

import com.pousada.domain.Money;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.persistence.Database;
import com.pousada.persistence.ReservationRepository;
import com.pousada.persistence.RoomRepository;

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
        return db.tx(c -> {
            List<Room> allRooms = rooms.findAll(c);
            Map<Integer, Room> byNumber = allRooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = reservations.findPaidOccupying(c, night);
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                csv.append(r.roomNumber()).append(',').append(byNumber.get(r.roomNumber()).type()).append(',')
                        .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
            }
            csv.append("total,").append(occupying.size()).append('/').append(allRooms.size()).append('\n');
            return csv.toString();
        });
    }

    public String revenue(YearMonth month) {
        return db.tx(c -> {
            List<Reservation> kept = reservations.findKeepingMoney(c, month.atDay(1), month.plusMonths(1).atDay(1));
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Money.ZERO;
            for (Reservation r : kept) {
                BigDecimal amount = r.amountPaid().subtract(r.refund() == null ? Money.ZERO : r.refund());
                sum = sum.add(amount);
                csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',').append(amount).append('\n');
            }
            csv.append("total,").append(sum).append('\n');
            return csv.toString();
        });
    }
}
