package pousada.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.service.ReservationRepository;

public class TourismTaxReport extends MonthlyReport {
    /** Valor da prefeitura por hóspede por noite; muda todo ano. */
    static final BigDecimal TAX_PER_GUEST_NIGHT = Money.of("4.50");

    public TourismTaxReport(ReservationRepository repository) {
        super(repository);
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    ReportData generate(YearMonth month) {
        List<Reservation> paid = startingIn(month).stream().filter(r -> r.status().isPaid()).toList();
        List<List<String>> rows = paid.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.room().number()),
                        r.checkIn().toString(), r.checkOut().toString(), String.valueOf(r.guests()),
                        String.valueOf(r.nights()), taxOf(r).toPlainString()))
                .toList();
        BigDecimal total = paid.stream().map(TourismTaxReport::taxOf).reduce(Money.ZERO, BigDecimal::add);
        return new ReportData(month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"), rows,
                List.of("total", total.toPlainString()));
    }

    private static BigDecimal taxOf(Reservation r) {
        return Money.round(TAX_PER_GUEST_NIGHT.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }
}
