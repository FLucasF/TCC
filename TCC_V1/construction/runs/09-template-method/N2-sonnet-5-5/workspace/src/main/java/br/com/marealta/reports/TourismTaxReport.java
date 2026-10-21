package br.com.marealta.reports;

import br.com.marealta.Money;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class TourismTaxReport extends MonthlyReport<Reservation> {
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationService reservations, BigDecimal taxPerGuestNight) {
        super(reservations);
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override
    public String name() { return "tourism-tax"; }

    @Override
    protected List<String> columns() {
        return List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax");
    }

    @Override
    protected List<Reservation> select(YearMonth month) {
        return startingIn(month, r -> r.getStatus().isPaid());
    }

    private BigDecimal taxOf(Reservation r) {
        return Money.round(taxPerGuestNight.multiply(BigDecimal.valueOf(r.getGuests() * r.getNights())));
    }

    @Override
    protected List<String> cells(Reservation r) {
        return List.of(String.valueOf(r.getId()), String.valueOf(r.getRoomNumber()), r.getCheckIn().toString(),
                r.getCheckOut().toString(), String.valueOf(r.getGuests()), String.valueOf(r.getNights()),
                Money.csv(taxOf(r)));
    }

    @Override
    protected List<String> total(YearMonth month, List<Reservation> items) {
        BigDecimal sum = items.stream().map(this::taxOf).reduce(Money.ZERO, BigDecimal::add);
        return List.of("total", Money.csv(sum));
    }
}
