package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.store.Tx;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class OccupancyReport extends DailyReport {
    @Override
    public String name() {
        return "occupancy";
    }

    @Override
    protected ReportTable build(LocalDate date, Tx tx) {
        List<Room> rooms = tx.rooms();
        List<Reservation> staying = tx.reservationsCoveringNight(date).stream()
                .filter(Reservation::paid)
                .sorted(Comparator.comparingInt(Reservation::roomNumber).thenComparingLong(Reservation::id))
                .toList();
        List<List<String>> rows = staying.stream().map(r -> List.of(
                String.valueOf(r.roomNumber()),
                rooms.stream().filter(room -> room.number() == r.roomNumber()).findFirst()
                        .map(room -> room.type().name()).orElse(""),
                String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString())).toList();
        long occupied = staying.stream().map(Reservation::roomNumber).distinct().count();
        return new ReportTable(name(), date.toString(),
                List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", occupied + "/" + rooms.size()));
    }
}
