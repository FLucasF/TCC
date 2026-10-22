package pousada.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.domain.Rooms;
import pousada.persistence.Database;
import pousada.persistence.ReservationRepository;

/** Os relatórios em CSV. */
public final class ReportService {
    private final Database db;
    private final ReservationRepository reservations = new ReservationRepository();

    public ReportService(Database db) {
        this.db = db;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = db.tx(c -> reservations.findPaidOnNight(c, night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(String.join(",", String.valueOf(r.roomNumber()),
                    Rooms.find(r.roomNumber()).orElseThrow().type().name(),
                    String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString())).append('\n');
        }
        return csv.append("total,").append(occupying.size()).append('/').append(Rooms.all().size())
                .append('\n').toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = db.tx(c ->
                reservations.findRetainedStartingBetween(c, month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(String.join(",", String.valueOf(r.id()), String.valueOf(r.roomNumber()),
                    r.checkIn().toString(), r.checkOut().toString(), r.status().name(),
                    r.retained().toPlainString())).append('\n');
            total = Money.round(total.add(r.retained()));
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
