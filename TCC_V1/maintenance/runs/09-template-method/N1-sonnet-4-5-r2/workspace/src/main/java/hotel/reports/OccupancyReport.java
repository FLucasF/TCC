package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Which rooms are occupied on a given night, for the front desk (CSV). */
public final class OccupancyReport extends CsvReport {

    private final Rooms rooms;
    private final ReservationStore store;
    private LocalDate night;

    public OccupancyReport(Rooms rooms, ReservationStore store) {
        this.rooms = rooms;
        this.store = store;
    }

    public String generate(LocalDate night) {
        this.night = night;
        return generate();
    }

    @Override
    protected String reportName() {
        return "occupancy";
    }

    @Override
    protected String period() {
        return night.toString();
    }

    @Override
    protected String headerLine() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> dataLines() {
        List<Reservation> occupying = occupyingReservations();
        List<String> lines = new ArrayList<>();
        for (Reservation reservation : occupying) {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            lines.add(room.number() + ","
                    + room.type() + ","
                    + reservation.id() + ","
                    + reservation.checkIn() + ","
                    + reservation.checkOut());
        }
        return lines;
    }

    @Override
    protected String totalLine() {
        int occupied = occupyingReservations().size();
        int total = rooms.all().size();
        return "total," + occupied + "/" + total;
    }

    private List<Reservation> occupyingReservations() {
        List<Reservation> occupying = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean coversNight = !reservation.checkIn().isAfter(night) && reservation.checkOut().isAfter(night);
            if (reservation.isPaid() && coversNight) {
                occupying.add(reservation);
            }
        }
        occupying.sort(Comparator.comparingInt(Reservation::roomNumber));
        return occupying;
    }
}
