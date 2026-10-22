package com.marealta.report;

import com.marealta.domain.Money;
import com.marealta.domain.Reservation;
import com.marealta.domain.Room;
import com.marealta.persistence.Database;
import com.marealta.persistence.ReservationRepository;
import com.marealta.persistence.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class Reports {
    private final Database database;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public Reports(Database database, RoomRepository rooms, ReservationRepository reservations) {
        this.database = database;
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        return database.transaction(c -> {
            List<Reservation> occupying = reservations.findPaidOccupying(c, night);
            int totalRooms = rooms.all(c).size();
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                Room room = r.room();
                csv.append(room.number()).append(',').append(room.type()).append(',').append(r.id()).append(',')
                        .append(r.checkInDate()).append(',').append(r.checkOutDate()).append('\n');
            }
            long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
            return csv.append("total,").append(occupiedRooms).append('/').append(totalRooms).append('\n')
                    .toString();
        });
    }

    public String revenue(YearMonth month) {
        return database.transaction(c -> {
            List<Reservation> starting = reservations.findRetainingMoneyStartingBetween(
                    c, month.atDay(1), month.plusMonths(1).atDay(1));
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Money.ZERO;
            for (Reservation r : starting) {
                csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkInDate())
                        .append(',').append(r.checkOutDate()).append(',').append(r.status()).append(',')
                        .append(r.retained().toPlainString()).append('\n');
                sum = sum.add(r.retained());
            }
            return csv.append("total,").append(Money.round(sum).toPlainString()).append('\n').toString();
        });
    }
}
