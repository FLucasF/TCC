"""Scenario 10 (negative control): the contained solution (one more column in the occupancy report) and the
over-engineered one (a configurable list of columns, "for when the phone comes")."""

from kit import OCCUPANCY, REPORTS_TEST, Solution


def update_tests(solution: Solution) -> None:
    solution.replace(REPORTS_TEST, "                room,type,reservation,check_in,check_out\n",
                     "                room,type,reservation,guest,check_in,check_out\n", count=2)
    solution.replace(REPORTS_TEST, "                101,STANDARD,2,2027-03-30", "                101,STANDARD,2,Bruno Lima,2027-03-30")
    solution.replace(REPORTS_TEST, "                201,DELUXE,1,2027-04-01", "                201,DELUXE,1,Bruno Lima,2027-04-01")
    solution.replace(REPORTS_TEST, "                301,SUITE,3,2027-04-02", "                301,SUITE,3,Bruno Lima,2027-04-02")


contained = Solution("10-contained")
contained.replace(OCCUPANCY, '        csv.append("room,type,reservation,check_in,check_out\\n");',
                  '        csv.append("room,type,reservation,guest,check_in,check_out\\n");')
contained.replace(OCCUPANCY, """                    .append(reservation.id()).append(',')
""", """                    .append(reservation.id()).append(',')
                    .append(reservation.guest().name().replace(',', ' ')).append(',')
""")
update_tests(contained)
contained.build_and_save()


over = Solution("10-overengineered")
over.write("src/main/java/hotel/reports/ReportColumn.java", """package hotel.reports;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.util.function.BiFunction;

/** One column of a report: its header and how to read its value. New columns (the phone...) are one more entry. */
public record ReportColumn(String header, BiFunction<Reservation, Room, Object> value) {

    String valueOf(Reservation reservation, Room room) {
        return String.valueOf(value.apply(reservation, room)).replace(',', ' ');
    }
}
""")
over.write(OCCUPANCY, """package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Which rooms are occupied on a given night, for the front desk (CSV). */
public final class OccupancyReport {

    private static final List<ReportColumn> COLUMNS = List.of(
            new ReportColumn("room", (reservation, room) -> room.number()),
            new ReportColumn("type", (reservation, room) -> room.type()),
            new ReportColumn("reservation", (reservation, room) -> reservation.id()),
            new ReportColumn("guest", (reservation, room) -> reservation.guest().name()),
            new ReportColumn("check_in", (reservation, room) -> reservation.checkIn()),
            new ReportColumn("check_out", (reservation, room) -> reservation.checkOut()));

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

        StringBuilder csv = new StringBuilder();
        csv.append(COLUMNS.stream().map(ReportColumn::header).collect(Collectors.joining(","))).append('\\n');
        for (Reservation reservation : occupying) {
            Room room = rooms.find(reservation.roomNumber()).orElseThrow();
            csv.append(COLUMNS.stream().map(column -> column.valueOf(reservation, room))
                    .collect(Collectors.joining(","))).append('\\n');
        }
        csv.append("total,").append(occupying.size()).append('/').append(rooms.all().size()).append('\\n');
        return csv.toString();
    }
}
""")
update_tests(over)
over.build_and_save()
