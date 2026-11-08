package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.store.Store;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OccupancyReport implements Report {
    private final Store store;

    public OccupancyReport(Store store) {
        this.store = store;
    }

    @Override public String name() { return "occupancy"; }

    @Override public String parameter() { return "date"; }

    @Override
    public ReportData generate(String period) {
        LocalDate night = Periods.date(period);
        return store.transaction(tx -> {
            List<Room> rooms = tx.rooms();
            Map<Integer, Room> byNumber = rooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = tx.occupying(night).stream().filter(r -> r.status().isPaid()).toList();
            List<List<String>> rows = occupying.stream().map(r -> List.of(
                    String.valueOf(r.roomNumber()), byNumber.get(r.roomNumber()).type().name(),
                    String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString())).toList();
            long occupied = occupying.stream().map(Reservation::roomNumber).distinct().count();
            return new ReportData(name(), night.toString(),
                    List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                    List.of("total", occupied + "/" + rooms.size()));
        });
    }
}
