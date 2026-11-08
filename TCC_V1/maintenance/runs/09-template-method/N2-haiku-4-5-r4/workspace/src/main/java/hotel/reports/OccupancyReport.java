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
public final class OccupancyReport extends Report {

    private final Rooms rooms;
    private final ReservationStore store;
    private final LocalDate night;
    private List<Reservation> occupying;

    public OccupancyReport(Rooms rooms, ReservationStore store) {
        this.rooms = rooms;
        this.store = store;
        this.night = null;
    }

    private OccupancyReport(Rooms rooms, ReservationStore store, LocalDate night) {
        this.rooms = rooms;
        this.store = store;
        this.night = night;
    }

    public String generate(LocalDate night) {
        return new OccupancyReport(rooms, store, night).generate();
    }

    @Override
    protected String title() {
        return POUSADA_NAME + ",occupancy," + night;
    }

    @Override
    protected String header() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected void data(List<String> lines) {
        occupying = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean coversNight = !reservation.checkIn().isAfter(night) && reservation.checkOut().isAfter(night);
            if (reservation.isPaid() && coversNight) {
                occupying.add(reservation);
            }
        }
        occupying.sort(Comparator.comparingInt(Reservation::roomNumber));

        for (Reservation reservation : occupying) {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            lines.add(room.number() + "," + room.type() + "," + reservation.id() + "," + reservation.checkIn()
                    + "," + reservation.checkOut());
        }
    }

    @Override
    protected String total() {
        return "total," + occupying.size() + "/" + rooms.all().size();
    }
}
