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
    protected ReportContent generate(YearMonth month) {
        List<Reservation> kept = withMoneyKept(month);
        List<List<String>> rows = kept.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                        r.checkOut().toString(), r.status().name(), Money.csv(r.kept())))
                .toList();
        BigDecimal total = kept.stream().map(Reservation::kept).reduce(Money.ZERO, BigDecimal::add);
        return new ReportContent(month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "status", "amount"),
                rows, List.of("total", Money.csv(total)));
    }
}
