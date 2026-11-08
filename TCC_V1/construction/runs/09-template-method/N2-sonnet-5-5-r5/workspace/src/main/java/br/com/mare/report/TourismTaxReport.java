package br.com.mare.report;

import br.com.mare.domain.Money;
import br.com.mare.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Taxa de turismo da prefeitura: um valor por hóspede por noite, que muda todo ano. */
public class TourismTaxReport extends MonthlyReport {
    private final ReservationQueries queries;
    private final BigDecimal ratePerGuestNight;

    public TourismTaxReport(ReservationQueries queries, BigDecimal ratePerGuestNight) {
        this.queries = queries;
        this.ratePerGuestNight = ratePerGuestNight;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected Body body(YearMonth month) {
        List<Reservation> list = queries.paidStartingIn(month);
        List<String> rows = list.stream()
                .map(r -> r.id() + "," + r.roomNumber() + "," + r.checkIn() + "," + r.checkOut()
                        + "," + r.guests() + "," + r.nights() + "," + Money.csv(taxOf(r)))
                .toList();
        BigDecimal total = list.stream().map(this::taxOf).reduce(Money.ZERO, BigDecimal::add);
        return new Body(rows, "total," + Money.csv(total));
    }

    private BigDecimal taxOf(Reservation r) {
        return Money.round(ratePerGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }
}
