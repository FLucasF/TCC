package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.infra.Db;
import br.com.marealta.infra.ReservationRepository;
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
        List<Reservation> occupying = db.transaction(connection -> reservations.findPaidOccupying(connection, night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.roomNumber()).append(',')
                    .append(Room.find(r.roomNumber()).orElseThrow().type()).append(',')
                    .append(r.id()).append(',')
                    .append(r.checkIn()).append(',')
                    .append(r.checkOut()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
        return csv.append("total,").append(occupiedRooms).append('/').append(Room.all().size()).append('\n')
                .toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> started = db.transaction(connection ->
                reservations.findWithRevenueStartingBetween(connection, month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : started) {
            csv.append(r.id()).append(',')
                    .append(r.roomNumber()).append(',')
                    .append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',')
                    .append(r.status()).append(',')
                    .append(r.netRevenue().toPlainString()).append('\n');
            total = total.add(r.netRevenue());
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
