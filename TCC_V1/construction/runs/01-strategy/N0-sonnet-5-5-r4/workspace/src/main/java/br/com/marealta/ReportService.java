package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Relatórios CSV de ocupação e financeiro. */
public final class ReportService {
    private final Db db;
    private final Repository repo;

    public ReportService(Db db, Repository repo) {
        this.db = db;
        this.repo = repo;
    }

    public String occupancy(LocalDate night) {
        return db.tx(c -> {
            List<Reservation> rows = repo.occupying(c, night);
            List<Room> rooms = repo.rooms(c);
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : rows) {
                Room room = rooms.stream().filter(x -> x.number() == r.roomNumber()).findFirst().orElseThrow();
                sb.append(r.roomNumber()).append(',').append(room.type()).append(',').append(r.id()).append(',')
                        .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
            }
            long occupied = rows.stream().map(Reservation::roomNumber).distinct().count();
            sb.append("total,").append(occupied).append('/').append(rooms.size()).append('\n');
            return sb.toString();
        });
    }

    public String revenue(YearMonth month) {
        return db.tx(c -> {
            List<Reservation> rows = repo.revenue(c, month.atDay(1), month.plusMonths(1).atDay(1));
            StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Money.ZERO;
            for (Reservation r : rows) {
                BigDecimal kept = r.amountPaid().subtract(r.refund() == null ? Money.ZERO : r.refund());
                sum = sum.add(kept);
                sb.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',').append(kept.toPlainString()).append('\n');
            }
            sb.append("total,").append(sum.toPlainString()).append('\n');
            return sb.toString();
        });
    }
}
