package pousada.service;

import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.persistence.Database;
import pousada.persistence.ReservationRepository;
import pousada.persistence.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportService {
    private final Database database;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public ReportService(Database database, RoomRepository rooms, ReservationRepository reservations) {
        this.database = database;
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        return database.inTransaction(c -> {
            List<Room> allRooms = rooms.findAll(c);
            Map<Integer, Room> byNumber = allRooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = reservations.findPaidOnNight(c, night);
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                csv.append(r.roomNumber()).append(',').append(byNumber.get(r.roomNumber()).type()).append(',')
                        .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
            }
            long occupiedRooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
            return csv.append("total,").append(occupiedRooms).append('/').append(allRooms.size()).append('\n').toString();
        });
    }

    public String revenue(YearMonth month) {
        return database.inTransaction(c -> {
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal total = Money.ZERO;
            for (Reservation r : reservations.findStartingIn(c, month)) {
                BigDecimal kept = r.amountKept();
                if (kept.signum() <= 0) continue;
                csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',').append(kept).append('\n');
                total = total.add(kept);
            }
            return csv.append("total,").append(Money.round(total)).append('\n').toString();
        });
    }
}
