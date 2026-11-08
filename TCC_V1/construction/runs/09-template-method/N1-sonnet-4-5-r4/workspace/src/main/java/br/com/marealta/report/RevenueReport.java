package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RevenueReport extends ReportGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;

    public RevenueReport(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "revenue";
    }

    @Override
    protected String buildHeader() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> buildDataRows(String period) {
        YearMonth yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));

        List<ReservationStatus> statuses = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.CHECKED_OUT,
            ReservationStatus.CANCELLED
        );

        List<Reservation> reservations = reservationRepository.findByMonthAndStatuses(
            yearMonth.getYear(), yearMonth.getMonthValue(), statuses
        );

        List<String> rows = new ArrayList<>();
        for (Reservation reservation : reservations) {
            BigDecimal netAmount = calculateNetAmount(reservation);
            rows.add(String.format("%d,%s,%s,%s,%s,%s",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(DATE_FORMATTER),
                reservation.getCheckOut().format(DATE_FORMATTER),
                reservation.getStatus(),
                formatDecimal(netAmount)
            ));
        }

        return rows;
    }

    @Override
    protected String buildTotal(String period) {
        YearMonth yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));

        List<ReservationStatus> statuses = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.CHECKED_OUT,
            ReservationStatus.CANCELLED
        );

        List<Reservation> reservations = reservationRepository.findByMonthAndStatuses(
            yearMonth.getYear(), yearMonth.getMonthValue(), statuses
        );

        BigDecimal total = reservations.stream()
            .map(this::calculateNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("total,%s", formatDecimal(total));
    }

    private BigDecimal calculateNetAmount(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refund = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refund);
    }

    private String formatDecimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
