package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Which rooms are occupied on a given night, for the front desk (CSV). */
public final class OccupancyReport extends Report<LocalDate, Reservation> {

    private final Rooms rooms;
    private final ReservationStore store;

    public OccupancyReport(Rooms rooms, ReservationStore store) {
        this.rooms = rooms;
        this.store = store;
    }

    @Override
    protected String name() {
        return "occupancy";
    }

    @Override
    protected String header() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<Reservation> rows(LocalDate night) {
        return store.all().stream()
                .filter(reservation -> reservation.isPaid()
                        && !reservation.checkIn().isAfter(night)
                        && reservation.checkOut().isAfter(night))
                .sorted(Comparator.comparingInt(Reservation::roomNumber))
                .toList();
    }

    @Override
    protected String line(Reservation reservation) {
        Room room = rooms.find(reservation.roomNumber()).orElseThrow();
        return room.number() + "," + room.type() + "," + reservation.id() + ","
                + reservation.checkIn() + "," + reservation.checkOut();
    }

    @Override
    protected String total(List<Reservation> occupying) {
        return "total," + occupying.size() + "/" + rooms.all().size();
    }
}
