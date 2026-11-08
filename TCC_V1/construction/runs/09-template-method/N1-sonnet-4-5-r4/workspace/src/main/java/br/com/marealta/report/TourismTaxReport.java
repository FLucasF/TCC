package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TourismTaxReport extends ReportGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationRepository reservationRepository;

    public TourismTaxReport(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "tourism-tax";
    }

    @Override
    protected String buildHeader() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> buildDataRows(String period) {
        YearMonth yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));

        List<ReservationStatus> paidStatuses = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.CHECKED_OUT
        );

        List<Reservation> reservations = reservationRepository.findByMonthAndStatuses(
            yearMonth.getYear(), yearMonth.getMonthValue(), paidStatuses
        );

        List<String> rows = new ArrayList<>();
        for (Reservation reservation : reservations) {
            BigDecimal tax = calculateTax(reservation);
            rows.add(String.format("%d,%s,%s,%s,%d,%d,%s",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(DATE_FORMATTER),
                reservation.getCheckOut().format(DATE_FORMATTER),
                reservation.getGuests(),
                reservation.getNights(),
                formatDecimal(tax)
            ));
        }

        return rows;
    }

    @Override
    protected String buildTotal(String period) {
        YearMonth yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));

        List<ReservationStatus> paidStatuses = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.CHECKED_OUT
        );

        List<Reservation> reservations = reservationRepository.findByMonthAndStatuses(
            yearMonth.getYear(), yearMonth.getMonthValue(), paidStatuses
        );

        BigDecimal total = reservations.stream()
            .map(this::calculateTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("total,%s", formatDecimal(total));
    }

    private BigDecimal calculateTax(Reservation reservation) {
        return TAX_PER_GUEST_PER_NIGHT
            .multiply(BigDecimal.valueOf(reservation.getGuests()))
            .multiply(BigDecimal.valueOf(reservation.getNights()));
    }

    private String formatDecimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
