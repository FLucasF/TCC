package br.com.marealta.report;

import br.com.marealta.app.ReservationRepository;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.RoomCatalog;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class OccupancyReport {
    private final RoomCatalog rooms;
    private final ReservationRepository reservations;

    public OccupancyReport(RoomCatalog rooms, ReservationRepository reservations) {
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String generate(LocalDate night) {
        List<Reservation> occupying = reservations.findAll().stream()
                .filter(r -> r.wasPaid() && r.occupies(night))
                .sorted(Comparator.comparingInt(Reservation::roomNumber))
                .toList();
        return Csv.build(
                List.of("room", "type", "reservation", "check_in", "check_out"),
                occupying,
                r -> List.of(r.roomNumber(), rooms.require(r.roomNumber()).type(), r.id(), r.checkIn(), r.checkOut()),
                occupying.size() + "/" + rooms.all().size());
    }
}
