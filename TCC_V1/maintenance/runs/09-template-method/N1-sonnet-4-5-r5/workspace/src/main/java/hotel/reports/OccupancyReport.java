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
public final class OccupancyReport {

    private final Rooms rooms;
    private final ReservationStore store;

    public OccupancyReport(Rooms rooms, ReservationStore store) {
        this.rooms = rooms;
        this.store = store;
    }

    public String generate(LocalDate night) {
        List<Reservation> occupying = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean coversNight = !reservation.checkIn().isAfter(night) && reservation.checkOut().isAfter(night);
            if (reservation.isPaid() && coversNight) {
                occupying.add(reservation);
            }
        }
        occupying.sort(Comparator.comparingInt(Reservation::roomNumber));

        StringBuilder dataLines = new StringBuilder();
        for (Reservation reservation : occupying) {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            dataLines.append(room.number()).append(',')
                    .append(room.type()).append(',')
                    .append(reservation.id()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append('\n');
        }

        String header = "room,type,reservation,check_in,check_out";
        String totalLine = "total," + occupying.size() + '/' + rooms.all().size();
        return new CsvReport("occupancy", night.toString()).format(header, dataLines.toString(), totalLine);
    }
}
