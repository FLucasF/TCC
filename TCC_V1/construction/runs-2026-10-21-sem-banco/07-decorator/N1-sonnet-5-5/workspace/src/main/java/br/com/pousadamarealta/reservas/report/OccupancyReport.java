package br.com.pousadamarealta.reservas.report;

import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.RoomCatalog;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class OccupancyReport {

    private OccupancyReport() {
    }

    public static String render(List<Reservation> reservations, RoomCatalog catalog, LocalDate night) {
        List<Reservation> occupying = reservations.stream()
                .filter(r -> r.status().isPaid() && r.occupies(night))
                .sorted(Comparator.comparingInt((Reservation r) -> r.room().number())
                        .thenComparingLong(Reservation::id))
                .toList();
        List<String> rows = occupying.stream()
                .map(r -> String.join(",", String.valueOf(r.room().number()), r.room().type().name(),
                        String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString()))
                .toList();
        long rooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return CsvReport.render("room,type,reservation,check_in,check_out", rows, rooms + "/" + catalog.size());
    }
}
