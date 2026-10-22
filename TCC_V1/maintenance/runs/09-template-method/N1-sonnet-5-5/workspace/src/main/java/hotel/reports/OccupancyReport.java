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
public final class OccupancyReport extends CsvReport<LocalDate> {

    private final Rooms rooms;
    private final ReservationStore store;

    public OccupancyReport(Rooms rooms, ReservationStore store) {
        super("occupancy");
        this.rooms = rooms;
        this.store = store;
    }

    @Override
    String header() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    List<String> lines(LocalDate night) {
        List<Reservation> occupying = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean coversNight = !reservation.checkIn().isAfter(night) && reservation.checkOut().isAfter(night);
            if (reservation.isPaid() && coversNight) {
                occupying.add(reservation);
            }
        }
        occupying.sort(Comparator.comparingInt(Reservation::roomNumber));

        List<String> lines = new ArrayList<>();
        for (Reservation reservation : occupying) {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            lines.add(room.number() + "," + room.type() + "," + reservation.id() + ","
                    + reservation.checkIn() + "," + reservation.checkOut());
        }
        lines.add("total," + occupying.size() + "/" + rooms.all().size());
        return lines;
    }
}
