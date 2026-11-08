package pousada.report;

import pousada.Reservation;
import pousada.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OccupancyReport extends DailyReport {
    private final ReportSource source;

    public OccupancyReport(ReportSource source) {
        super("occupancy");
        this.source = source;
    }

    @Override
    protected ReportData build(LocalDate night) {
        List<Room> rooms = source.rooms();
        Map<Integer, Room> byNumber = rooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
        List<Reservation> occupying = source.paidOccupying(night);
        List<List<String>> rows = occupying.stream().map(r -> List.of(
                String.valueOf(r.roomNumber()),
                byNumber.get(r.roomNumber()).type().name(),
                String.valueOf(r.id()),
                r.checkIn().toString(),
                r.checkOut().toString())).toList();
        long occupied = occupying.stream().map(Reservation::roomNumber).distinct().count();
        return new ReportData(List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", occupied + "/" + rooms.size()));
    }
}
