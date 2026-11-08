package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Relatórios em CSV. */
public class ReportService {
    private final Database db;
    private final Rooms rooms;
    private final ReservationRepository reservations;

    public ReportService(Database db, Rooms rooms, ReservationRepository reservations) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = db.inTransaction(c -> reservations.occupying(c, night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.roomNumber()).append(',').append(rooms.get(r.roomNumber()).type()).append(',')
                    .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        return csv.append("total,").append(occupying.size()).append('/').append(rooms.all().size())
                .append('\n').toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = db.inTransaction(c ->
                reservations.retainingStartingBetween(c, month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.retained().toPlainString()).append('\n');
            total = Money.round(total.add(r.retained()));
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
