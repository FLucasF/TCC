package pousada.report;

import pousada.domain.Reservation;
import pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

public class RevenueReport implements Report {
    private final YearMonth month;
    private final List<Reservation> reservations;

    public RevenueReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservations = reservationRepository.findRevenueReservationsForMonth(month);
    }

    @Override
    public String getName() {
        return "revenue";
    }

    @Override
    public String getPeriod() {
        return month.toString();
    }

    @Override
    public List<String> getColumns() {
        return List.of("reservation", "room", "check_in", "check_out", "status", "amount");
    }

    @Override
    public List<Row> getDataRows() {
        return reservations.stream()
            .map(r -> new Row(List.of(
                r.getId().toString(),
                r.getRoomNumber(),
                r.getCheckIn().toString(),
                r.getCheckOut().toString(),
                r.getStatus().name(),
                formatAmount(getNetAmount(r))
            )))
            .collect(Collectors.toList());
    }

    @Override
    public Row getTotalRow() {
        BigDecimal total = reservations.stream()
            .map(this::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Row(List.of("total", formatAmount(total)));
    }

    private BigDecimal getNetAmount(Reservation r) {
        BigDecimal refund = r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO;
        return r.getAmountPaid().subtract(refund);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}
