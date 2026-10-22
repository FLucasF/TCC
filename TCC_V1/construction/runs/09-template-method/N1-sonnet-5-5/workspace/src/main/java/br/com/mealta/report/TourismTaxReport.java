package br.com.mealta.report;

import br.com.mealta.domain.Money;
import br.com.mealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Taxa de turismo das reservas pagas que começam no mês. */
public class TourismTaxReport implements Report {
    private final YearMonth month;
    private final BigDecimal ratePerGuestNight;
    private final List<Reservation> paid;

    public TourismTaxReport(YearMonth month, BigDecimal ratePerGuestNight, List<Reservation> startingInMonth) {
        this.month = month;
        this.ratePerGuestNight = ratePerGuestNight;
        this.paid = startingInMonth.stream().filter(Reservation::isPaid).toList();
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    public String period() {
        return month.toString();
    }

    @Override
    public List<String> columns() {
        return List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax");
    }

    @Override
    public List<List<String>> rows() {
        return paid.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.roomNumber()),
                        r.checkInDate().toString(), r.checkOutDate().toString(),
                        String.valueOf(r.guests()), String.valueOf(r.nights()), Money.plain(taxOf(r))))
                .toList();
    }

    @Override
    public List<String> total() {
        BigDecimal sum = paid.stream().map(this::taxOf).reduce(Money.ZERO, BigDecimal::add);
        return List.of("total", Money.plain(sum));
    }

    private BigDecimal taxOf(Reservation r) {
        return Money.round(ratePerGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }
}
