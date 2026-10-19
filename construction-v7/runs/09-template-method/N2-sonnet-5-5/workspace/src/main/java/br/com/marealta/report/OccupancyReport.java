package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.service.RoomCatalog;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

public class OccupancyReport extends CsvReport<LocalDate> {
    private final RoomCatalog rooms;

    public OccupancyReport(RoomCatalog rooms) { this.rooms = rooms; }

    @Override public String name() { return "occupancy"; }

    @Override public String parameter() { return "date"; }

    @Override
    protected LocalDate parse(String period) {
        try {
            return LocalDate.parse(period);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("Data inválida: " + period);
        }
    }

    @Override
    protected Table build(LocalDate night, List<Reservation> all) {
        List<Reservation> occupying = all.stream()
                .filter(r -> r.isPaid() && r.occupiesNight(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()).thenComparingInt(Reservation::id))
                .toList();
        List<List<String>> rows = occupying.stream().map(r -> List.of(
                String.valueOf(r.room().number()), r.room().type().name(), String.valueOf(r.id()),
                r.checkInDate().toString(), r.checkOutDate().toString())).toList();
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return new Table(List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", occupiedRooms + "/" + rooms.all().size()));
    }
}
