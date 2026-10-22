package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.RoomCatalog;
import br.com.marealta.persistence.ReservationRepository;

import java.time.LocalDate;
import java.util.List;

public class OccupancyReport extends CsvReport {
    private final List<Reservation> occupying;

    public OccupancyReport(ReservationRepository repo, LocalDate night) {
        this.occupying = repo.paidOnNight(night);
    }

    @Override protected String header() { return "room,type,reservation,check_in,check_out"; }

    @Override protected List<String> rows() {
        return occupying.stream()
                .map(r -> r.roomNumber() + "," + RoomCatalog.find(r.roomNumber()).orElseThrow().type() + ","
                        + r.id() + "," + r.checkInDate() + "," + r.checkOutDate())
                .toList();
    }

    @Override protected String total() { return "total," + occupying.size() + "/" + RoomCatalog.all().size(); }
}
