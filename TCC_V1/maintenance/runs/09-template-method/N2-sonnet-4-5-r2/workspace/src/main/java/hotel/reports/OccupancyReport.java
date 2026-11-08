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
public final class OccupancyReport extends ReportTemplate<LocalDate> {

    private final Rooms rooms;
    private final ReservationStore store;

    public OccupancyReport(Rooms rooms, ReservationStore store) {
        this.rooms = rooms;
        this.store = store;
    }

    @Override
    protected String reportName() {
        return "occupancy";
    }

    @Override
    protected String headerLine() {
        return "room,type,reservation,check_in,check_out\n";
    }

    @Override
    protected String dataLines(LocalDate night) {
        List<Reservation> occupying = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean coversNight = !reservation.checkIn().isAfter(night) && reservation.checkOut().isAfter(night);
            if (reservation.isPaid() && coversNight) {
                occupying.add(reservation);
            }
        }
        occupying.sort(Comparator.comparingInt(Reservation::roomNumber));

        StringBuilder lines = new StringBuilder();
        for (Reservation reservation : occupying) {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            lines.append(room.number()).append(',')
                    .append(room.type()).append(',')
                    .append(reservation.id()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append('\n');
        }
        this.totalCount = occupying.size();
        return lines.toString();
    }

    @Override
    protected String totalLine(LocalDate night) {
        return "total," + totalCount + '/' + rooms.all().size() + '\n';
    }

    private int totalCount;
}
