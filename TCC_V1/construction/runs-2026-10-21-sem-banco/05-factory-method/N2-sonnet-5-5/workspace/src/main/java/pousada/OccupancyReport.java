package pousada;

import java.util.Comparator;
import java.util.List;

public class OccupancyReport extends CsvReport {
    private final List<Reservation> occupying;
    private final int roomCount;

    public OccupancyReport(List<Reservation> occupying, int roomCount) {
        this.occupying = occupying.stream().sorted(Comparator.comparingInt(r -> r.room().number())).toList();
        this.roomCount = roomCount;
    }

    protected List<String> columns() {
        return List.of("room", "type", "reservation", "check_in", "check_out");
    }

    protected List<List<String>> rows() {
        return occupying.stream().map(r -> List.of(String.valueOf(r.room().number()), r.room().type().name(),
                String.valueOf(r.id()), r.checkInDate().toString(), r.checkOutDate().toString())).toList();
    }

    protected String total() {
        return occupying.size() + "/" + roomCount;
    }
}
