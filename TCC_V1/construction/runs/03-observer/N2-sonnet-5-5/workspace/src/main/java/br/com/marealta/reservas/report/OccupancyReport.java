package br.com.marealta.reservas.report;

import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.RoomCatalog;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class OccupancyReport {
    private final RoomCatalog rooms;

    public OccupancyReport(RoomCatalog rooms) {
        this.rooms = rooms;
    }

    public String generate(List<Reservation> reservations, LocalDate night) {
        List<Reservation> occupying = reservations.stream()
                .filter(r -> r.occupiesNight(night))
                .sorted(Comparator.comparingInt(r -> r.room().number()))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id())
                    .append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return csv.append("total,").append(occupiedRooms).append('/').append(rooms.all().size()).append('\n').toString();
    }
}
