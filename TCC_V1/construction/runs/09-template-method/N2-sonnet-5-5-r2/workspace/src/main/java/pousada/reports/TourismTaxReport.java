package pousada.reports;

import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.repo.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Taxa de turismo da prefeitura: valor por hóspede por noite (muda todo ano, por isso é configurável). */
public class TourismTaxReport extends MonthReport {
    private final BigDecimal perGuestNight;

    public TourismTaxReport(ReservationRepository reservations, BigDecimal perGuestNight) {
        super(reservations);
        this.perGuestNight = perGuestNight;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    private BigDecimal taxOf(Reservation r) {
        return Money.round(perGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }

    @Override
    protected Body body(YearMonth month) {
        List<Reservation> list = startingIn(month).stream().filter(r -> r.status().isPaid()).toList();
        List<String> rows = list.stream().map(r -> String.join(",",
                String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                r.checkOut().toString(), String.valueOf(r.guests()), String.valueOf(r.nights()),
                Money.plain(taxOf(r)))).toList();
        BigDecimal sum = list.stream().map(this::taxOf).reduce(Money.ZERO, BigDecimal::add);
        return new Body(rows, "total," + Money.plain(sum));
    }
}
