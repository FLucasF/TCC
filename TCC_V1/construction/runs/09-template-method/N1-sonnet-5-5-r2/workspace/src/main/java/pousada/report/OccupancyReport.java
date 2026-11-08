package pousada.report;

import pousada.domain.Reservation;
import pousada.domain.RoomCatalog;

import java.util.List;

public class OccupancyReport implements Report {
    private final RoomCatalog rooms;

    public OccupancyReport(RoomCatalog rooms) {
        this.rooms = rooms;
    }

    @Override
    public String name() {
        return "occupancy";
    }

    @Override
    public PeriodKind periodKind() {
        return PeriodKind.DAY;
    }

    @Override
    public ReportTable build(Period period, ReportSource source) {
        List<Reservation> occupying = source.paidOccupying(period.from());
        List<List<String>> rows = occupying.stream().map(r -> List.of(
                String.valueOf(r.roomNumber()),
                rooms.get(r.roomNumber()).type().name(),
                String.valueOf(r.id()),
                r.checkIn().toString(),
                r.checkOut().toString())).toList();
        return new ReportTable(
                List.of("room", "type", "reservation", "check_in", "check_out"),
                rows,
                List.of("total", occupying.size() + "/" + rooms.size()));
    }
}
