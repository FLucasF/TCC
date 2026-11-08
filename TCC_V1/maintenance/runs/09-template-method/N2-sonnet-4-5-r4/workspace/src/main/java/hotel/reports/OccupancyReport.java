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
    private List<Reservation> occupying;

    public OccupancyReport(Rooms rooms, ReservationStore store) {
        this.rooms = rooms;
        this.store = store;
    }

    public String generate(LocalDate night) {
        occupying = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean coversNight = !reservation.checkIn().isAfter(night) && reservation.checkOut().isAfter(night);
            if (reservation.isPaid() && coversNight) {
                occupying.add(reservation);
            }
        }
        occupying.sort(Comparator.comparingInt(Reservation::roomNumber));
        return assemble(night.toString());
    }

    @Override
    protected String reportName() {
        return "occupancy";
    }

    @Override
    protected String headers() {
        return "room,type,reservation,check_in,check_out\n";
    }

    @Override
    protected String dataLines() {
        StringBuilder lines = new StringBuilder();
        for (Reservation reservation : occupying) {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            lines.append(room.number()).append(',')
                    .append(room.type()).append(',')
                    .append(reservation.id()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append('\n');
        }
        return lines.toString();
    }

    @Override
    protected String totalLine() {
        return "total," + occupying.size() + '/' + rooms.all().size() + '\n';
    }
}
