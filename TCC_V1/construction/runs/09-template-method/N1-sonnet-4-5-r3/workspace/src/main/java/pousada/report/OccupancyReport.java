package pousada.report;

import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.repository.ReservationRepository;
import pousada.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class OccupancyReport implements Report {
    private final LocalDate date;
    private final List<Reservation> reservations;
    private final int totalRooms;

    public OccupancyReport(LocalDate date, ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.date = date;
        this.reservations = reservationRepository.findPaidReservationsForDate(date);
        this.totalRooms = roomRepository.findAll().size();
    }

    @Override
    public String getName() {
        return "occupancy";
    }

    @Override
    public String getPeriod() {
        return date.toString();
    }

    @Override
    public List<String> getColumns() {
        return List.of("room", "type", "reservation", "check_in", "check_out");
    }

    @Override
    public List<Row> getDataRows() {
        return reservations.stream()
            .map(r -> new Row(List.of(
                r.getRoomNumber(),
                getRoomType(r.getRoomNumber()),
                r.getId().toString(),
                r.getCheckIn().toString(),
                r.getCheckOut().toString()
            )))
            .collect(Collectors.toList());
    }

    @Override
    public Row getTotalRow() {
        return new Row(List.of("total", String.format("%d/%d", reservations.size(), totalRooms)));
    }

    private String getRoomType(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}
