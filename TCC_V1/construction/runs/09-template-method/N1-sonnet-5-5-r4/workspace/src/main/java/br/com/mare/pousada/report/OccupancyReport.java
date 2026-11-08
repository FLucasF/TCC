package br.com.mare.pousada.report;

import br.com.mare.pousada.domain.Reservation;
import br.com.mare.pousada.domain.Room;
import br.com.mare.pousada.domain.RoomCatalog;
import br.com.mare.pousada.infra.ReservationRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class OccupancyReport extends Report<LocalDate> {
    private final ReservationRepository reservations;

    public OccupancyReport(ReservationRepository reservations) {
        this.reservations = reservations;
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
            return LocalDate.parse(raw, STRICT_DATE);
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida: " + raw + " (use 2027-07-10)");
        }
    }

    @Override
    protected Table build(Connection connection, LocalDate night) throws SQLException {
        List<List<String>> rows = reservations.findStayingOn(connection, night).stream()
                .filter(r -> r.getStatus().isPaid())
                .map(this::row)
                .toList();
        return new Table(List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", rows.size() + "/" + RoomCatalog.all().size()));
    }

    private List<String> row(Reservation r) {
        Room room = RoomCatalog.require(r.getRoomNumber());
        return List.of(String.valueOf(room.number()), room.type().name(), String.valueOf(r.getId()),
                r.getCheckIn().toString(), r.getCheckOut().toString());
    }
}
