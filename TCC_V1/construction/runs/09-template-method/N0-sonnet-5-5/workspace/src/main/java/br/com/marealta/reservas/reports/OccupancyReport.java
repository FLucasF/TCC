package br.com.marealta.reservas.reports;

import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class OccupancyReport implements Report {
    private final ReservationService service;

    public OccupancyReport(ReservationService service) {
        this.service = service;
    }

    @Override
    public String name() {
        return "occupancy";
    }

    @Override
    public String periodParam() {
        return "date";
    }

    @Override
    public ReportTable build(String period) {
        LocalDate date = Periods.date(period);
        List<Reservation> occupying = service.all().stream()
                .filter(r -> r.status().isPaid() && r.occupiesNight(date))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()).thenComparingInt(Reservation::id))
                .toList();
        List<List<String>> rows = occupying.stream().map(r -> List.of(
                String.valueOf(r.room().number()), r.room().type().name(), String.valueOf(r.id()),
                r.checkIn().toString(), r.checkOut().toString())).toList();
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return new ReportTable(name(), date.toString(),
                List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", occupiedRooms + "/" + service.rooms().size()));
    }
}
