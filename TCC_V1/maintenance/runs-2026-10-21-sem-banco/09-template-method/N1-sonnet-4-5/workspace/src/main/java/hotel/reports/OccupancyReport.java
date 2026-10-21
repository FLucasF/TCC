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

        CsvReport report = new CsvReport("occupancy", night.toString(),
                List.of("room", "type", "reservation", "check_in", "check_out"));
        for (Reservation reservation : occupying) {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            report.addRow(
                    String.valueOf(room.number()),
                    room.type().toString(),
                    String.valueOf(reservation.id()),
                    reservation.checkIn().toString(),
                    reservation.checkOut().toString());
        }
        report.setTotal("total", occupying.size() + "/" + rooms.all().size());
        return report.render();
    }
}
