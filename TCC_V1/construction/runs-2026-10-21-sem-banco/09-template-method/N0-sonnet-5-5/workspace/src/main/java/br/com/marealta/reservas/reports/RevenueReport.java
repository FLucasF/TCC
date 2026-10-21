package br.com.marealta.reservas.reports;

import br.com.marealta.reservas.domain.Money;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport implements Report {
    private final ReservationService service;

    public RevenueReport(ReservationService service) {
        this.service = service;
    }

    @Override
    public String name() {
        return "revenue";
    }

    @Override
    public String periodParam() {
        return "month";
    }

    @Override
    public ReportTable build(String period) {
        YearMonth month = Periods.month(period);
        List<Reservation> kept = ReservationQueries.keptMoneyStartingIn(service, month);
        List<List<String>> rows = kept.stream().map(r -> List.of(
                String.valueOf(r.id()), String.valueOf(r.room().number()),
                r.checkIn().toString(), r.checkOut().toString(), r.status().name(),
                Money.plain(r.amountKept()))).toList();
        BigDecimal total = kept.stream().map(Reservation::amountKept).reduce(Money.ZERO, BigDecimal::add);
        return new ReportTable(name(), month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", Money.plain(total)));
    }
}
