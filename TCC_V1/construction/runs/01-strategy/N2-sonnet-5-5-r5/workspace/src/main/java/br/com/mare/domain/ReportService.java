package br.com.mare.domain;

import br.com.mare.infra.Db;
import br.com.mare.infra.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final Db db;
    private final ReservationRepository reservations;

    public ReportService(Db db, ReservationRepository reservations) {
        this.db = db;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> list = db.transaction(c -> reservations.findPaidOccupying(c, night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : list) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id()).append(',')
                    .append(r.checkInDate()).append(',').append(r.checkOutDate()).append('\n');
        }
        long rooms = list.stream().map(r -> r.room().number()).distinct().count();
        return csv.append("total,").append(rooms).append('/').append(Rooms.all().size()).append('\n').toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> list = db.transaction(c ->
                reservations.findWithRevenueStartingBetween(c, month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : list) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkInDate()).append(',')
                    .append(r.checkOutDate()).append(',').append(r.status()).append(',')
                    .append(r.netRevenue().toPlainString()).append('\n');
            sum = sum.add(r.netRevenue());
        }
        return csv.append("total,").append(Money.round(sum).toPlainString()).append('\n').toString();
    }
}
