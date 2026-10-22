package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.RoomCatalog;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class OccupancyReport {
    private OccupancyReport() {}

    /** Recebe as reservas que cobrem a noite; só as pagas contam. */
    public static String render(LocalDate night, List<Reservation> covering) {
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        List<Reservation> occupying = covering.stream()
                .filter(r -> r.status().isPaid())
                .sorted(Comparator.comparingInt(Reservation::roomNumber).thenComparing(Reservation::id))
                .toList();
        for (Reservation r : occupying) {
            csv.append(r.roomNumber()).append(',')
                    .append(RoomCatalog.find(r.roomNumber()).orElseThrow().type()).append(',')
                    .append(r.id()).append(',')
                    .append(r.checkIn()).append(',')
                    .append(r.checkOut()).append('\n');
        }
        long rooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
        return csv.append("total,").append(rooms).append('/').append(RoomCatalog.all().size()).append('\n').toString();
    }
}
