package pousada.reports;

import pousada.domain.Reservation;
import pousada.domain.RoomCatalog;
import pousada.repo.ReservationRepository;

import java.time.LocalDate;
import java.util.List;

public class OccupancyReport extends Report<LocalDate> {
    private final ReservationRepository reservations;

    public OccupancyReport(ReservationRepository reservations) {
        this.reservations = reservations;
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
    protected LocalDate parsePeriod(String raw) {
        return LocalDate.parse(raw);
    }

    @Override
    protected String header() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected Body body(LocalDate night) {
        List<Reservation> occupying = reservations.findPaidOccupying(night);
        List<String> rows = occupying.stream().map(r -> String.join(",",
                String.valueOf(r.roomNumber()), RoomCatalog.require(r.roomNumber()).type().name(),
                String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString())).toList();
        long rooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
        return new Body(rows, "total," + rooms + "/" + RoomCatalog.all().size());
    }
}
