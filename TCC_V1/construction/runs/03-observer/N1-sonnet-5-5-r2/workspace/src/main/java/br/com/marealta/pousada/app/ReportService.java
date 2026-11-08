package br.com.marealta.pousada.app;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {

    private final Database db;
    private final ReservationRepository reservations;

    public ReportService(Database db, ReservationRepository reservations) {
        this.db = db;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = db.transaction(() -> reservations.findPaidOccupying(night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id())
                    .append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        return csv.append("total,").append(occupying.size()).append('/').append(Room.ALL.size()).append('\n')
                .toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = db.transaction(() ->
                reservations.findRetainingMoney(month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn())
                    .append(',').append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(r.retainedAmount().toPlainString()).append('\n');
            total = total.add(r.retainedAmount());
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
