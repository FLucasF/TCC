package pousada.report;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import pousada.db.ReservationRepository;
import pousada.db.RoomRepository;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.error.ApiException;

public class OccupancyReport implements Report {
    private final ReservationRepository reservations;
    private final RoomRepository rooms;

    public OccupancyReport(ReservationRepository reservations, RoomRepository rooms) {
        this.reservations = reservations;
        this.rooms = rooms;
    }

    @Override
    public String name() {
        return "occupancy";
    }

    @Override
    public String parameter() {
        return "date";
    }

    @Override
    public ReportContent generate(String period) {
        LocalDate night;
        try {
            night = LocalDate.parse(period);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalidRequest("Data fora do formato AAAA-MM-DD");
        }
        List<Room> allRooms = rooms.all();
        Map<Integer, Room> byNumber = allRooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
        List<Reservation> occupying = reservations.paidOccupying(night);
        List<List<String>> rows = occupying.stream()
                .map(r -> List.of(String.valueOf(r.roomNumber()), byNumber.get(r.roomNumber()).type().name(),
                        String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString()))
                .toList();
        return new ReportContent(night.toString(),
                List.of("room", "type", "reservation", "check_in", "check_out"),
                rows,
                List.of("total", occupying.size() + "/" + allRooms.size()));
    }
}
