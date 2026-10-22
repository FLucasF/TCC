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
public final class OccupancyReport extends Report<OccupancyReport.OccupancyRow> {

    record OccupancyRow(int roomNumber, String roomType, long reservationId,
                        LocalDate checkIn, LocalDate checkOut) {}

    private final Rooms rooms;
    private final ReservationStore store;

    public OccupancyReport(Rooms rooms, ReservationStore store) {
        this.rooms = rooms;
        this.store = store;
    }

    public String generate(LocalDate night) {
        return generateCsv(night);
    }

    @Override
    protected String reportName() {
        return "occupancy";
    }

    @Override
    protected List<OccupancyRow> collectData(Object period) {
        LocalDate night = (LocalDate) period;
        List<Reservation> occupying = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean coversNight = !reservation.checkIn().isAfter(night) && reservation.checkOut().isAfter(night);
            if (reservation.isPaid() && coversNight) {
                occupying.add(reservation);
            }
        }
        occupying.sort(Comparator.comparingInt(Reservation::roomNumber));

        List<OccupancyRow> rows = new ArrayList<>();
        for (Reservation reservation : occupying) {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            rows.add(new OccupancyRow(room.number(), room.type().toString(),
                    reservation.id(), reservation.checkIn(), reservation.checkOut()));
        }
        return rows;
    }

    @Override
    protected String headerLine() {
        return "room,type,reservation,check_in,check_out\n";
    }

    @Override
    protected String dataLine(OccupancyRow row) {
        return row.roomNumber + "," + row.roomType + "," + row.reservationId + ","
                + row.checkIn + "," + row.checkOut + "\n";
    }

    @Override
    protected String totalLine(List<OccupancyRow> data) {
        return "total," + data.size() + "/" + rooms.all().size() + "\n";
    }
}
