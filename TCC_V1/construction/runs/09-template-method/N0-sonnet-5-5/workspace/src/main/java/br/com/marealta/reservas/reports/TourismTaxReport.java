package br.com.marealta.reservas.reports;

import br.com.marealta.reservas.domain.Money;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class TourismTaxReport implements Report {
    private final ReservationService service;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationService service, BigDecimal taxPerGuestNight) {
        this.service = service;
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    public String periodParam() {
        return "month";
    }

    private BigDecimal tax(Reservation r) {
        return Money.round(taxPerGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }

    @Override
    public ReportTable build(String period) {
        YearMonth month = Periods.month(period);
        List<Reservation> paid = ReservationQueries.paidStartingIn(service, month);
        List<List<String>> rows = paid.stream().map(r -> List.of(
                String.valueOf(r.id()), String.valueOf(r.room().number()),
                r.checkIn().toString(), r.checkOut().toString(),
                String.valueOf(r.guests()), String.valueOf(r.nights()),
                Money.plain(tax(r)))).toList();
        BigDecimal total = paid.stream().map(this::tax).reduce(Money.ZERO, BigDecimal::add);
        return new ReportTable(name(), month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"), rows,
                List.of("total", Money.plain(total)));
    }
}
