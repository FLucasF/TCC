package pousada.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import pousada.db.ReservationRepository;
import pousada.domain.Money;
import pousada.domain.Reservation;

public class TourismTaxReport extends MonthlyReport {
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationRepository reservations, BigDecimal taxPerGuestNight) {
        super(reservations);
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    protected ReportContent generate(YearMonth month) {
        List<Reservation> paid = reservations.startingIn(month).stream().filter(r -> r.status().paid()).toList();
        List<List<String>> rows = paid.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                        r.checkOut().toString(), String.valueOf(r.guests()), String.valueOf(r.nights()),
                        Money.csv(taxOf(r))))
                .toList();
        BigDecimal total = paid.stream().map(this::taxOf).reduce(Money.ZERO, BigDecimal::add);
        return new ReportContent(month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"),
                rows, List.of("total", Money.csv(total)));
    }

    private BigDecimal taxOf(Reservation r) {
        return Money.round(taxPerGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }
}
