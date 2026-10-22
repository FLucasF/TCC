package pousada.report;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import pousada.db.ReservationRepository;
import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.domain.Reservation;
import pousada.domain.RoomCatalog;

public class OccupancyReport extends Report<LocalDate> {
    private final ReservationRepository reservations;
    private final RoomCatalog rooms;

    public OccupancyReport(ReservationRepository reservations, RoomCatalog rooms) {
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
    protected LocalDate parse(String raw) {
        try {
            if (raw == null) {
                throw new DateTimeParseException("data", "", 0);
            }
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Data inválida, use o formato 2027-07-10");
        }
    }

    @Override
    protected Table table(LocalDate night) {
        List<Reservation> occupying = reservations.findAll().stream()
                .filter(r -> r.occupies(night))
                .sorted(Comparator.comparingInt(Reservation::getRoomNumber).thenComparing(Reservation::getId))
                .toList();
        List<String> rows = occupying.stream()
                .map(r -> String.join(",", String.valueOf(r.getRoomNumber()),
                        rooms.get(r.getRoomNumber()).type().name(), String.valueOf(r.getId()),
                        r.getCheckIn().toString(), r.getCheckOut().toString()))
                .toList();
        long occupiedRooms = occupying.stream().map(Reservation::getRoomNumber).distinct().count();
        return new Table("room,type,reservation,check_in,check_out", rows,
                "total," + occupiedRooms + "/" + rooms.all().size());
    }
}
