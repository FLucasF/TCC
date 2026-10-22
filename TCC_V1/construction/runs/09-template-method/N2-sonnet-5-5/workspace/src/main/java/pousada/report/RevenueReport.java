package pousada.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import pousada.db.ReservationRepository;
import pousada.domain.Money;
import pousada.domain.Reservation;

public class RevenueReport extends MonthlyReport {
    public RevenueReport(ReservationRepository reservations) {
        super(reservations);
    }

    @Override
    public String name() {
        return "revenue";
    }

    @Override
    protected Table table(YearMonth month) {
        List<Reservation> list = startingIn(month, MonthlyReport::keptMoney);
        List<String> rows = list.stream()
                .map(r -> String.join(",", String.valueOf(r.getId()), String.valueOf(r.getRoomNumber()),
                        r.getCheckIn().toString(), r.getCheckOut().toString(), r.getStatus().name(),
                        Money.plain(r.retained())))
                .toList();
        BigDecimal sum = list.stream().map(Reservation::retained).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Table("reservation,room,check_in,check_out,status,amount", rows, "total," + Money.plain(sum));
    }
}
