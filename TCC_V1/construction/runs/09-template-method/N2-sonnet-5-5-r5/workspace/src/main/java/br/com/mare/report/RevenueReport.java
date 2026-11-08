package br.com.mare.report;

import br.com.mare.domain.Money;
import br.com.mare.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport extends MonthlyReport {
    private final ReservationQueries queries;

    public RevenueReport(ReservationQueries queries) {
        this.queries = queries;
    }

    @Override
    public String name() {
        return "revenue";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected Body body(YearMonth month) {
        List<Reservation> list = queries.retainedStartingIn(month);
        List<String> rows = list.stream()
                .map(r -> r.id() + "," + r.roomNumber() + "," + r.checkIn() + "," + r.checkOut()
                        + "," + r.status() + "," + Money.csv(r.retained()))
                .toList();
        BigDecimal total = list.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
        return new Body(rows, "total," + Money.csv(total));
    }
}
