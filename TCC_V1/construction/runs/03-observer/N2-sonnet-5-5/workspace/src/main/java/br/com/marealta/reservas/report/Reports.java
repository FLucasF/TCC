package br.com.marealta.reservas.report;

import br.com.marealta.reservas.domain.Money;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.Room;
import br.com.marealta.reservas.persistence.Database;
import br.com.marealta.reservas.persistence.ReservationRepository;
import br.com.marealta.reservas.persistence.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Reports {
    private final Database db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public Reports(Database db, RoomRepository rooms, ReservationRepository reservations) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        return db.inTransaction(() -> {
            List<Room> allRooms = rooms.findAll();
            Map<Integer, Room> byNumber = allRooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = reservations.findPaidOccupying(night);
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                csv.append(r.roomNumber()).append(',').append(byNumber.get(r.roomNumber()).type()).append(',')
                        .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut())
                        .append('\n');
            }
            long occupiedRooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
            csv.append("total,").append(occupiedRooms).append('/').append(allRooms.size()).append('\n');
            return csv.toString();
        });
    }

    public String revenue(YearMonth month) {
        return db.inTransaction(() -> {
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal total = Money.ZERO;
            for (Reservation r : reservations.findKeptStartingIn(month)) {
                csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',')
                        .append(r.kept().toPlainString()).append('\n');
                total = total.add(r.kept());
            }
            csv.append("total,").append(Money.round(total).toPlainString()).append('\n');
            return csv.toString();
        });
    }
}
