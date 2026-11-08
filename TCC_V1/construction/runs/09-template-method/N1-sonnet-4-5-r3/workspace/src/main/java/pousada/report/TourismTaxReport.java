package pousada.report;

import pousada.domain.Reservation;
import pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

public class TourismTaxReport implements Report {
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final YearMonth month;
    private final List<Reservation> reservations;

    public TourismTaxReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservations = reservationRepository.findPaidReservationsStartingInMonth(month);
    }

    @Override
    public String getName() {
        return "tourism-tax";
    }

    @Override
    public String getPeriod() {
        return month.toString();
    }

    @Override
    public List<String> getColumns() {
        return List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax");
    }

    @Override
    public List<Row> getDataRows() {
        return reservations.stream()
            .map(r -> new Row(List.of(
                r.getId().toString(),
                r.getRoomNumber(),
                r.getCheckIn().toString(),
                r.getCheckOut().toString(),
                String.valueOf(r.getGuests()),
                String.valueOf(r.getNights()),
                formatAmount(calculateTax(r))
            )))
            .collect(Collectors.toList());
    }

    @Override
    public Row getTotalRow() {
        BigDecimal total = reservations.stream()
            .map(this::calculateTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Row(List.of("total", formatAmount(total)));
    }

    private BigDecimal calculateTax(Reservation r) {
        return TAX_PER_GUEST_PER_NIGHT
            .multiply(BigDecimal.valueOf(r.getGuests()))
            .multiply(BigDecimal.valueOf(r.getNights()));
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}
