package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.persistence.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport extends CsvReport {
    private final List<Reservation> kept;

    public RevenueReport(ReservationRepository repo, YearMonth month) {
        this.kept = repo.startingBetween(month.atDay(1), month.plusMonths(1).atDay(1)).stream()
                .filter(r -> r.retainedAmount().signum() > 0)
                .toList();
    }

    @Override protected String header() { return "reservation,room,check_in,check_out,status,amount"; }

    @Override protected List<String> rows() {
        return kept.stream()
                .map(r -> r.id() + "," + r.roomNumber() + "," + r.checkInDate() + "," + r.checkOutDate() + ","
                        + r.status() + "," + r.retainedAmount().toPlainString())
                .toList();
    }

    @Override protected String total() {
        BigDecimal sum = kept.stream().map(Reservation::retainedAmount).reduce(Money.ZERO, BigDecimal::add);
        return "total," + sum.toPlainString();
    }
}
