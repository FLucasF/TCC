package br.com.maretalta.report;

import br.com.maretalta.ReservationService;
import br.com.maretalta.domain.Reservation;
import br.com.maretalta.domain.Room;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class OccupancyReport implements Report {
    private final ReservationService service;

    public OccupancyReport(ReservationService service) {
        this.service = service;
    }

    @Override public String name() { return "occupancy"; }

    @Override public String paramName() { return "date"; }

    @Override
    public String generate(String rawParam) {
        LocalDate date = Csv.parseDate(rawParam);
        List<Reservation> occupying = service.read(all -> all.stream()
                .filter(r -> r.status.isPaid() && !date.isBefore(r.checkIn) && date.isBefore(r.checkOut))
                .sorted(Comparator.<Reservation>comparingInt(r -> r.room.number()).thenComparingInt(r -> r.id))
                .toList());
        List<String> rows = occupying.stream()
                .map(r -> String.join(",", String.valueOf(r.room.number()), r.room.type().name(),
                        String.valueOf(r.id), r.checkIn.toString(), r.checkOut.toString()))
                .toList();
        long occupiedRooms = occupying.stream().map(r -> r.room.number()).distinct().count();
        List<Room> rooms = service.rooms();
        return Csv.build(name(), date.toString(), "room,type,reservation,check_in,check_out", rows,
                "total," + occupiedRooms + "/" + rooms.size());
    }
}
