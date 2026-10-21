package br.com.marealta.reports;

import br.com.marealta.ApiException;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;
import br.com.marealta.Rooms;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

public class OccupancyReport extends Report<LocalDate, Reservation> {
    private final ReservationService reservations;
    private final Rooms rooms;

    public OccupancyReport(ReservationService reservations, Rooms rooms) {
        this.reservations = reservations;
        this.rooms = rooms;
    }

    @Override
    public String name() { return "occupancy"; }

    @Override
    public String paramName() { return "date"; }

    @Override
    protected LocalDate parse(String raw) {
        try {
            if (raw == null) throw new DateTimeParseException("", "", 0);
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Data inválida, use o formato 2027-07-10");
        }
    }

    @Override
    protected List<String> columns() {
        return List.of("room", "type", "reservation", "check_in", "check_out");
    }

    @Override
    protected List<Reservation> select(LocalDate night) {
        return reservations.select(r -> r.getStatus().isPaid() && r.occupiesNight(night)).stream()
                .sorted(Comparator.comparing(Reservation::getRoomNumber)).toList();
    }

    @Override
    protected List<String> cells(Reservation r) {
        return List.of(String.valueOf(r.getRoomNumber()), r.room().type().name(), String.valueOf(r.getId()),
                r.getCheckIn().toString(), r.getCheckOut().toString());
    }

    @Override
    protected List<String> total(LocalDate night, List<Reservation> items) {
        long occupied = items.stream().map(Reservation::getRoomNumber).distinct().count();
        return List.of("total", occupied + "/" + rooms.all().size());
    }
}
