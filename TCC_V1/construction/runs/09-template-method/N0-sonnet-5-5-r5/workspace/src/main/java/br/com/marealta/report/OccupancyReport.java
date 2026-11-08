package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.store.Tx;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OccupancyReport implements Report {
    @Override
    public String name() { return "occupancy"; }

    @Override
    public String param() { return "date"; }

    @Override
    public ReportTable build(Tx tx, String rawPeriod) {
        LocalDate night = Periods.date(rawPeriod);
        List<Room> rooms = tx.rooms();
        Map<Integer, Room> byNumber = rooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
        List<Reservation> staying = tx.stayingOn(night).stream().filter(r -> r.status().isPaid()).toList();
        List<List<String>> rows = staying.stream()
                .map(r -> List.of(String.valueOf(r.roomNumber()), byNumber.get(r.roomNumber()).type().name(),
                        String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString()))
                .toList();
        return new ReportTable(name(), night.toString(),
                List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", staying.size() + "/" + rooms.size()));
    }
}
