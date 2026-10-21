package br.com.marealta.reports;

import br.com.marealta.Money;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport extends MonthlyReport<Reservation> {
    public RevenueReport(ReservationService reservations) {
        super(reservations);
    }

    @Override
    public String name() { return "revenue"; }

    @Override
    protected List<String> columns() {
        return List.of("reservation", "room", "check_in", "check_out", "status", "amount");
    }

    @Override
    protected List<Reservation> select(YearMonth month) {
        return retainingMoney(month);
    }

    @Override
    protected List<String> cells(Reservation r) {
        return List.of(String.valueOf(r.getId()), String.valueOf(r.getRoomNumber()), r.getCheckIn().toString(),
                r.getCheckOut().toString(), r.getStatus().name(), Money.csv(r.retained()));
    }

    @Override
    protected List<String> total(YearMonth month, List<Reservation> items) {
        BigDecimal sum = items.stream().map(Reservation::retained).reduce(Money.ZERO, BigDecimal::add);
        return List.of("total", Money.csv(sum));
    }
}
