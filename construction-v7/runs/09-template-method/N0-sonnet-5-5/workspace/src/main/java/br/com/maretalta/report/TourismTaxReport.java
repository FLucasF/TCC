package br.com.maretalta.report;

import br.com.maretalta.ReservationService;
import br.com.maretalta.domain.Money;
import br.com.maretalta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class TourismTaxReport implements Report {
    private final ReservationService service;
    private final TourismTaxPolicy policy;

    public TourismTaxReport(ReservationService service, TourismTaxPolicy policy) {
        this.service = service;
        this.policy = policy;
    }

    @Override public String name() { return "tourism-tax"; }

    @Override public String paramName() { return "month"; }

    private BigDecimal taxOf(Reservation r) {
        return Money.round(policy.ratePerGuestNight(r.checkIn).multiply(BigDecimal.valueOf((long) r.guests * r.nights)));
    }

    @Override
    public String generate(String rawParam) {
        YearMonth month = Csv.parseMonth(rawParam);
        return service.read(all -> {
            List<Reservation> rs = Csv.startingIn(all, month).stream().filter(r -> r.status.isPaid()).toList();
            List<String> rows = rs.stream()
                    .map(r -> String.join(",", String.valueOf(r.id), String.valueOf(r.room.number()),
                            r.checkIn.toString(), r.checkOut.toString(), String.valueOf(r.guests),
                            String.valueOf(r.nights), Money.plain(taxOf(r))))
                    .toList();
            BigDecimal sum = rs.stream().map(this::taxOf).reduce(Money.ZERO, BigDecimal::add);
            return Csv.build(name(), month.toString(), "reservation,room,check_in,check_out,guests,nights,tax",
                    rows, "total," + Money.plain(sum));
        });
    }
}
