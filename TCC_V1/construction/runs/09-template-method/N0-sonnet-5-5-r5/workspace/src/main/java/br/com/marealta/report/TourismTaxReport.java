package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.TourismTax;
import br.com.marealta.store.Tx;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class TourismTaxReport implements Report {
    private final TourismTax tax;

    public TourismTaxReport(TourismTax tax) {
        this.tax = tax;
    }

    @Override
    public String name() { return "tourism-tax"; }

    @Override
    public String param() { return "month"; }

    @Override
    public ReportTable build(Tx tx, String rawPeriod) {
        YearMonth month = Periods.month(rawPeriod);
        List<Reservation> paid = Ledger.paidStartingIn(tx, month);
        BigDecimal sum = Money.ZERO;
        for (Reservation r : paid) {
            sum = sum.add(tax.forReservation(r));
        }
        List<List<String>> rows = paid.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                        r.checkOut().toString(), String.valueOf(r.guests()), String.valueOf(r.nights()),
                        Money.plain(tax.forReservation(r))))
                .toList();
        return new ReportTable(name(), month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"), rows,
                List.of("total", Money.plain(sum)));
    }
}
