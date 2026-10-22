package br.com.marealta.service;

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
        return database.inTransaction(connection -> {
            Map<Integer, Room> byNumber = rooms.findAll(connection).stream()
                    .collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = reservations.findPaidOccupying(connection, night);

            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                csv.append(r.roomNumber()).append(',').append(byNumber.get(r.roomNumber()).type()).append(',')
                        .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
            }
            long occupiedRooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
            return csv.append("total,").append(occupiedRooms).append('/').append(byNumber.size()).append('\n')
                    .toString();
        });
    }

    public String revenue(YearMonth month) {
        return database.inTransaction(connection -> {
            List<Reservation> starting = reservations.findCheckingInBetween(
                    connection, month.atDay(1), month.plusMonths(1).atDay(1));

            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal total = Money.ZERO;
            for (Reservation r : starting) {
                BigDecimal amount = r.netAmount();
                if (amount.signum() <= 0) {
                    continue;
                }
                csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',').append(amount).append('\n');
                total = total.add(amount);
            }
            return csv.append("total,").append(total).append('\n').toString();
        });
    }
}
