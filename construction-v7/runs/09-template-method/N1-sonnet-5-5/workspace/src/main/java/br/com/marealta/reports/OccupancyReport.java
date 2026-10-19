package br.com.marealta.reports;

import br.com.marealta.ApiException;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;
import br.com.marealta.Room;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

public class OccupancyReport extends CsvReport<LocalDate> {
    private final ReservationService reservations;

    public OccupancyReport(ReservationService reservations) {
        super("occupancy", "date");
        this.reservations = reservations;
    }

    @Override
    LocalDate parse(String value) {
        try {
            if (value != null) return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            // cai no erro abaixo
        }
        throw ApiException.invalid("date deve estar no formato yyyy-MM-dd");
    }

    @Override
    String label(LocalDate date) {
        return date.toString();
    }

    @Override
    Table table(LocalDate night) {
        List<Reservation> occupying = reservations.all().stream()
                .filter(r -> r.status().isPaid())
                .filter(r -> !night.isBefore(r.checkIn()) && night.isBefore(r.checkOut()))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()))
                .toList();
        List<List<String>> rows = occupying.stream()
                .map(r -> List.of(String.valueOf(r.room().number()), r.room().type().name(),
                        String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString()))
                .toList();
        long rooms = occupying.stream().map(Reservation::room).distinct().count();
        return new Table(List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", rooms + "/" + Room.ALL.size()));
    }
}
