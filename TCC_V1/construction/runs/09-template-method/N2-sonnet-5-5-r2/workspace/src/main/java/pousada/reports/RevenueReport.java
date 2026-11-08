package pousada.reports;

import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.repo.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport extends MonthReport {
    public RevenueReport(ReservationRepository reservations) {
        super(reservations);
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
        List<Reservation> list = withRetainedMoney(month);
        List<String> rows = list.stream().map(r -> String.join(",",
                String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                r.checkOut().toString(), r.status().name(), Money.plain(r.retained()))).toList();
        BigDecimal sum = list.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
        return new Body(rows, "total," + Money.plain(sum));
    }
}
