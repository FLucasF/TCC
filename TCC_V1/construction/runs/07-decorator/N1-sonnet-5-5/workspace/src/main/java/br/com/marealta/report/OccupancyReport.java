package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.persistence.Db;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.persistence.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OccupancyReport {
    private static final List<String> HEADER = List.of("room", "type", "reservation", "check_in", "check_out");

    private final Db db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public OccupancyReport(Db db, RoomRepository rooms, ReservationRepository reservations) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String generate(LocalDate night) {
        return db.transaction(connection -> {
            List<Room> allRooms = rooms.findAll(connection);
            Map<Integer, Room> byNumber = allRooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = reservations.findPaidOccupying(connection, night);
            List<List<String>> rows = occupying.stream()
                    .map(r -> List.of(String.valueOf(r.roomNumber()), byNumber.get(r.roomNumber()).type(),
                            String.valueOf(r.id()), r.arrival().toString(), r.departure().toString()))
                    .toList();
            long occupiedRooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
            return Csv.render(HEADER, rows, occupiedRooms + "/" + allRooms.size());
        });
    }
}
