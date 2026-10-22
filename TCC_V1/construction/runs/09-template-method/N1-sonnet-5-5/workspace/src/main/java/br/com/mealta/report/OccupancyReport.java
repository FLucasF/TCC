package br.com.mealta.report;

import br.com.mealta.domain.Reservation;
import br.com.mealta.domain.RoomCatalog;

import java.time.LocalDate;
import java.util.List;

/** Reservas pagas que ocupam um quarto na noite, já em ordem de quarto. */
public class OccupancyReport implements Report {
    private final LocalDate night;
    private final List<Reservation> occupying;
    private final RoomCatalog rooms;

    public OccupancyReport(LocalDate night, List<Reservation> occupying, RoomCatalog rooms) {
        this.night = night;
        this.occupying = occupying;
        this.rooms = rooms;
    }

    @Override
    public String name() {
        return "occupancy";
    }

    @Override
    public String period() {
        return night.toString();
    }

    @Override
    public List<String> columns() {
        return List.of("room", "type", "reservation", "check_in", "check_out");
    }

    @Override
    public List<List<String>> rows() {
        return occupying.stream()
                .map(r -> List.of(String.valueOf(r.roomNumber()), rooms.require(r.roomNumber()).type().name(),
                        String.valueOf(r.id()), r.checkInDate().toString(), r.checkOutDate().toString()))
                .toList();
    }

    @Override
    public List<String> total() {
        long occupied = occupying.stream().map(Reservation::roomNumber).distinct().count();
        return List.of("total", occupied + "/" + rooms.all().size());
    }
}
