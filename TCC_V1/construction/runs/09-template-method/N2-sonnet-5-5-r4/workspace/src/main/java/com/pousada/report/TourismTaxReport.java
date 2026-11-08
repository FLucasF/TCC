package com.pousada.report;

import com.pousada.domain.Money;
import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Taxa de turismo cobrada pela prefeitura, por hóspede por noite, sobre as reservas pagas. */
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
    protected List<String> header() {
        return List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax");
    }

    @Override
    protected Body body(YearMonth month) {
        List<Reservation> paid = startingIn(month).stream().filter(r -> r.getStatus().paid()).toList();
        List<List<String>> rows = paid.stream().map(r -> List.of(
                String.valueOf(r.getId()), String.valueOf(r.getRoomNumber()), r.getCheckIn().toString(),
                r.getCheckOut().toString(), String.valueOf(r.getGuests()), String.valueOf(r.getNights()),
                Money.plain(taxOf(r)))).toList();
        BigDecimal total = paid.stream().map(this::taxOf).reduce(Money.ZERO, BigDecimal::add);
        return new Body(rows, List.of("total", Money.plain(total)));
    }

    private BigDecimal taxOf(Reservation r) {
        return Money.round(taxPerGuestNight.multiply(BigDecimal.valueOf((long) r.getGuests() * r.getNights())));
    }
}
