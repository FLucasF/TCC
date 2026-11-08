package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Store;
import java.math.BigDecimal;
import java.util.List;

/** Taxa de turismo da prefeitura: valor por hóspede por noite, vindo da configuração. */
public class TourismTaxReport extends MonthlyReport {
    private final BigDecimal perGuestNight;

    public TourismTaxReport(Store store, BigDecimal perGuestNight) {
        super(store);
        this.perGuestNight = perGuestNight;
    }

    @Override public String name() { return "tourism-tax"; }

    @Override
    protected ReportData build(String period, List<Reservation> startingInMonth) {
        List<Reservation> paid = startingInMonth.stream().filter(r -> r.status().isPaid()).toList();
        BigDecimal sum = Money.ZERO;
        List<List<String>> rows = new java.util.ArrayList<>();
        for (Reservation r : paid) {
            BigDecimal tax = Money.round(perGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
            sum = Money.round(sum.add(tax));
            rows.add(List.of(String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                    r.checkOut().toString(), String.valueOf(r.guests()), String.valueOf(r.nights()),
                    Money.plain(tax)));
        }
        return new ReportData(name(), period,
                List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"), rows,
                List.of("total", Money.plain(sum)));
    }
}
