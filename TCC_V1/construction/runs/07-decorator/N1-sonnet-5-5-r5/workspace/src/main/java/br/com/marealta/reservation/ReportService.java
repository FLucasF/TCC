package br.com.marealta.reservation;

import br.com.marealta.Money;
import br.com.marealta.db.Database;

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
        return database.run(connection -> {
            List<Room> allRooms = rooms.all(connection);
            Map<Integer, Room> byNumber = allRooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = reservations.paidOnNight(connection, night);
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
        return database.run(connection -> {
            List<Reservation> kept = reservations.keptStartingBetween(connection, month.atDay(1), month.atEndOfMonth());
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal total = Money.ZERO;
            for (Reservation r : kept) {
                csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',').append(r.kept()).append('\n');
                total = Money.round(total.add(r.kept()));
            }
            return csv.append("total,").append(total).append('\n').toString();
        });
    }
}
