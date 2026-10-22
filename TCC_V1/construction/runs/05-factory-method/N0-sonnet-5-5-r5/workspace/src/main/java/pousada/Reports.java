package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class Reports {
    private final Store store;

    public Reports(Store store) {
        this.store = store;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> rows = store.tx(c -> store.query(c,
                "WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND check_in <= ? AND check_out > ?",
                night, night));
        rows = rows.stream().sorted(Comparator.comparingInt(Reservation::roomNumber)
                .thenComparingLong(Reservation::id)).toList();
        StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : rows) {
            sb.append(r.roomNumber()).append(',').append(r.roomType()).append(',').append(r.id()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long rooms = rows.stream().map(Reservation::roomNumber).distinct().count();
        sb.append("total,").append(rooms).append('/').append(Room.ALL.size()).append('\n');
        return sb.toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> rows = store.tx(c -> store.query(c,
                "WHERE check_in >= ? AND check_in <= ? AND amount_paid - COALESCE(refund, 0) > 0 "
                        + "ORDER BY check_in, id",
                month.atDay(1), month.atEndOfMonth()));
        StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : rows) {
            sb.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',').append(r.kept().toPlainString())
                    .append('\n');
            sum = sum.add(r.kept());
        }
        sb.append("total,").append(Money.round(sum).toPlainString()).append('\n');
        return sb.toString();
    }
}
