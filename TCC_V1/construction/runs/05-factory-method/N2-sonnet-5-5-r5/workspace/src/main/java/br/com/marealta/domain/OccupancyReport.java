package br.com.marealta.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class OccupancyReport extends CsvReport {
    private final List<Reservation> occupying;

    public OccupancyReport(List<Reservation> candidates, LocalDate night) {
        this.occupying = candidates.stream()
                .filter(r -> r.status().isPaid() && r.occupies(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()))
                .toList();
    }

    @Override
    protected String header() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> rows() {
        return occupying.stream()
                .map(r -> r.room().number() + "," + r.room().type() + "," + r.id() + "," + r.checkIn() + ","
                        + r.checkOut())
                .toList();
    }

    @Override
    protected String totalLine() {
        long rooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return "total," + rooms + "/" + RoomCatalog.all().size();
    }
}
