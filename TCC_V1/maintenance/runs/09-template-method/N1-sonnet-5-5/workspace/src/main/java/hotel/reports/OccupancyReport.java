package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Which rooms are occupied on a given night, for the front desk (CSV). */
public final class OccupancyReport extends CsvReport<LocalDate> {

    private final Rooms rooms;
    private final ReservationStore store;

    public OccupancyReport(Rooms rooms, ReservationStore store) {
        this.rooms = rooms;
        this.store = store;
    }

    @Override
    public String name() {
        return "occupancy";
    }

    @Override
    protected Table table(LocalDate night) {
        List<Reservation> occupying = store.all().stream()
                .filter(reservation -> reservation.isPaid()
                        && !reservation.checkIn().isAfter(night)
                        && reservation.checkOut().isAfter(night))
                .sorted(Comparator.comparingInt(Reservation::roomNumber))
                .toList();

        List<String> rows = occupying.stream().map(reservation -> {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            return room.number() + "," + room.type() + "," + reservation.id() + ","
                    + reservation.checkIn() + "," + reservation.checkOut();
        }).toList();
        return new Table(
                "room,type,reservation,check_in,check_out",
                rows,
                "total," + occupying.size() + "/" + rooms.all().size());
    }
}
