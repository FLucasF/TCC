package br.com.marealta;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.persistence.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return database.inTransaction(c -> {
            List<Room> allRooms = rooms.findAll(c);
            Map<Integer, Room> byNumber = allRooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = reservations.paidOccupying(c, night);
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                csv.append(r.roomNumber()).append(',').append(byNumber.get(r.roomNumber()).type()).append(',')
                        .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
            }
            long occupiedRooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
            return csv.append("total,").append(occupiedRooms).append('/').append(allRooms.size()).append('\n')
                    .toString();
        });
    }

    public String revenue(YearMonth month) {
        return database.inTransaction(c -> {
            List<Reservation> list = reservations.withRevenueStartingBetween(c, month.atDay(1),
                    month.plusMonths(1).atDay(1));
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal total = Money.ZERO;
            for (Reservation r : list) {
                csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',')
                        .append(r.revenue().toPlainString()).append('\n');
                total = total.add(r.revenue());
            }
            return csv.append("total,").append(Money.round(total).toPlainString()).append('\n').toString();
        });
    }
}
