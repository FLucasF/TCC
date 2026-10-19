package br.com.marealta.reports;

import br.com.marealta.Money;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Taxa de turismo da prefeitura: valor por hóspede por noite, definido na configuração. */
public class TourismTaxReport extends MonthReport {
    private final BigDecimal ratePerGuestNight;

    public TourismTaxReport(ReservationService reservations, BigDecimal ratePerGuestNight) {
        super("tourism-tax", reservations);
        this.ratePerGuestNight = ratePerGuestNight;
    }

    private BigDecimal taxOf(Reservation r) {
        return Money.round(ratePerGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }

    @Override
    Table table(YearMonth month) {
        List<Reservation> paid = startingIn(month).stream().filter(r -> r.status().isPaid()).toList();
        List<List<String>> rows = paid.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.room().number()),
                        r.checkIn().toString(), r.checkOut().toString(),
                        String.valueOf(r.guests()), String.valueOf(r.nights()), Money.csv(taxOf(r))))
                .toList();
        BigDecimal total = paid.stream().map(this::taxOf).reduce(Money.round(BigDecimal.ZERO), BigDecimal::add);
        return new Table(List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"),
                rows, List.of("total", Money.csv(total)));
    }
}
