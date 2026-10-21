package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.service.ReservationService;
import br.com.marealta.service.RoomCatalog;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class OccupancyReport {
    private final ReservationService reservations;
    private final RoomCatalog rooms;

    public OccupancyReport(ReservationService reservations, RoomCatalog rooms) {
        this.reservations = reservations;
        this.rooms = rooms;
    }

    public String generate(LocalDate night) {
        List<Reservation> occupying = reservations.query(all -> all.stream()
                .filter(r -> r.status().isPaid() && r.occupies(night))
                .sorted(Comparator.comparingInt(r -> r.room().number()))
                .toList());
        List<List<String>> rows = occupying.stream()
                .map(r -> List.of(String.valueOf(r.room().number()), r.room().type().name(),
                        String.valueOf(r.id()), r.checkInDate().toString(), r.checkOutDate().toString()))
                .toList();
        return Csv.render(List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", occupying.size() + "/" + rooms.all().size()));
    }
}
