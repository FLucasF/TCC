package br.com.maretalta.report;

import br.com.maretalta.domain.Money;
import br.com.maretalta.domain.Reservation;
import br.com.maretalta.store.Store;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Taxa de turismo da prefeitura: um valor por hóspede por noite (muda todo ano, por isso é parâmetro). */
public class TourismTaxReport extends MonthlyReport {
    private final BigDecimal perGuestNight;

    public TourismTaxReport(BigDecimal perGuestNight) {
        this.perGuestNight = Money.round(perGuestNight);
    }

    @Override public String name() { return "tourism-tax"; }

    BigDecimal taxOf(Reservation r) {
        return Money.round(perGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }

    @Override
    protected Report build(Store.Tx tx, YearMonth month) {
        List<Reservation> reservations = paid(tx, month);
        List<List<String>> rows = reservations.stream().map(r -> List.of(
                String.valueOf(r.id()), String.valueOf(r.roomNumber()), r.checkIn().toString(),
                r.checkOut().toString(), String.valueOf(r.guests()), String.valueOf(r.nights()),
                Money.plain(taxOf(r)))).toList();
        BigDecimal total = reservations.stream().map(this::taxOf).reduce(Money.ZERO, BigDecimal::add);
        return new Report(name(), month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"), rows,
                List.of("total", Money.plain(total)));
    }
}
