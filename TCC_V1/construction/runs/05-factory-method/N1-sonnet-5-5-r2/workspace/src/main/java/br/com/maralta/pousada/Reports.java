package br.com.maralta.pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class Reports {
    private final Database database;
    private final ReservationRepository reservations = new ReservationRepository();

    public Reports(Database database) {
        this.database = database;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = database.inTransaction(c -> reservations.findPaidOccupying(c, night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.roomNumber()).append(',').append(Rooms.get(r.roomNumber()).type()).append(',')
                    .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long occupied = occupying.stream().map(Reservation::roomNumber).distinct().count();
        return csv.append("total,").append(occupied).append('/').append(Rooms.ALL.size()).append('\n').toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> starting = database.inTransaction(
                c -> reservations.findStartingBetween(c, month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : starting) {
            csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.retained().toPlainString()).append('\n');
            total = Money.round(total.add(r.retained()));
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
