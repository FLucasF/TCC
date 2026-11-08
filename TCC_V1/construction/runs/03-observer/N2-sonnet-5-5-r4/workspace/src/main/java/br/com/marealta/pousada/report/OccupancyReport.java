package br.com.marealta.pousada.report;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.RoomCatalog;

public final class OccupancyReport {

    private OccupancyReport() {
    }

    public static String render(LocalDate night, List<Reservation> reservations, RoomCatalog rooms) {
        List<Reservation> occupying = reservations.stream()
                .filter(r -> r.status().isPaid() && r.occupiesNight(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number()))
                .toList();
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',')
                    .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return csv.append("total,").append(occupiedRooms).append('/').append(rooms.size()).append('\n').toString();
    }
}
