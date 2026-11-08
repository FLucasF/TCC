package br.com.marealta.report;

import br.com.marealta.db.ReservationRepository;
import br.com.marealta.db.RoomRepository;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OccupancyReport implements Report {

    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public OccupancyReport(RoomRepository rooms, ReservationRepository reservations) {
        this.rooms = rooms;
        this.reservations = reservations;
    }

    @Override public String name() { return "occupancy"; }

    @Override public PeriodKind periodKind() { return PeriodKind.DATE; }

    @Override
    public ReportTable build(Connection c, String period) throws SQLException {
        List<Room> allRooms = rooms.findAll(c);
        Map<Integer, Room> byNumber = allRooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
        List<Reservation> occupying = reservations.findOccupying(c, PeriodKind.date(period)).stream()
                .filter(Reservation::isPaid).toList();
        List<List<String>> rows = new ArrayList<>();
        for (Reservation r : occupying) {
            rows.add(List.of(String.valueOf(r.roomNumber()), byNumber.get(r.roomNumber()).type().name(),
                    String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString()));
        }
        return new ReportTable(name(), period, List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", occupying.size() + "/" + allRooms.size()));
    }
}
