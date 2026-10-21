package pousada.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.service.ReservationRepository;

public class RevenueReport extends MonthlyReport {
    public RevenueReport(ReservationRepository repository) {
        super(repository);
    }

    @Override
    public String name() {
        return "revenue";
    }

    @Override
    ReportData generate(YearMonth month) {
        List<Reservation> kept = keptMoneyStartingIn(month);
        List<List<String>> rows = kept.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.room().number()),
                        r.checkIn().toString(), r.checkOut().toString(), r.status().name(),
                        r.heldAmount().toPlainString()))
                .toList();
        BigDecimal total = kept.stream().map(Reservation::heldAmount).reduce(Money.ZERO, BigDecimal::add);
        return new ReportData(month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", total.toPlainString()));
    }
}
