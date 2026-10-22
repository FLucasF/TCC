package pousada.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import pousada.db.ReservationRepository;
import pousada.domain.Money;
import pousada.domain.Reservation;

public class TourismTaxReport extends MonthlyReport {
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationRepository reservations, BigDecimal taxPerGuestNight) {
        super(reservations);
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    private BigDecimal tax(Reservation r) {
        return Money.round(taxPerGuestNight.multiply(BigDecimal.valueOf((long) r.getGuests() * r.getNights())));
    }

    @Override
    protected Table table(YearMonth month) {
        List<Reservation> list = startingIn(month, Reservation::paid);
        List<String> rows = list.stream()
                .map(r -> String.join(",", String.valueOf(r.getId()), String.valueOf(r.getRoomNumber()),
                        r.getCheckIn().toString(), r.getCheckOut().toString(), String.valueOf(r.getGuests()),
                        String.valueOf(r.getNights()), Money.plain(tax(r))))
                .toList();
        BigDecimal sum = list.stream().map(this::tax).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Table("reservation,room,check_in,check_out,guests,nights,tax", rows,
                "total," + Money.plain(sum));
    }
}
