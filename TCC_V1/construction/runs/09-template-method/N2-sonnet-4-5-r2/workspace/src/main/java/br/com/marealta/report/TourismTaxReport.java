package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TourismTaxReport extends CsvReport {
    private static final BigDecimal TAX_PER_GUEST_NIGHT = new BigDecimal("4.50");
    private final YearMonth month;
    private final ReservationRepository reservationRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

    public TourismTaxReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "tourism-tax";
    }

    @Override
    protected String getPeriod() {
        return month.format(monthFormatter);
    }

    @Override
    protected String buildHeaderLine() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> buildDataLines() {
        List<Reservation> reservations = getPaidReservationsStartingInMonth();
        List<String> lines = new ArrayList<>();

        for (Reservation reservation : reservations) {
            BigDecimal tax = calculateTax(reservation);
            lines.add(String.join(",",
                    String.valueOf(reservation.getId()),
                    String.valueOf(reservation.getRoomNumber()),
                    reservation.getCheckIn().format(dateFormatter),
                    reservation.getCheckOut().format(dateFormatter),
                    String.valueOf(reservation.getGuests()),
                    String.valueOf(reservation.getNights()),
                    formatAmount(tax)
            ));
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        List<Reservation> reservations = getPaidReservationsStartingInMonth();
        BigDecimal total = reservations.stream()
                .map(this::calculateTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return "total," + formatAmount(total);
    }

    private List<Reservation> getPaidReservationsStartingInMonth() {
        List<Reservation> all = reservationRepository.findByCheckInMonth(month);
        return all.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED
                        || r.getStatus() == ReservationStatus.CHECKED_IN
                        || r.getStatus() == ReservationStatus.CHECKED_OUT)
                .toList();
    }

    private BigDecimal calculateTax(Reservation reservation) {
        return TAX_PER_GUEST_NIGHT
                .multiply(BigDecimal.valueOf(reservation.getGuests()))
                .multiply(BigDecimal.valueOf(reservation.getNights()));
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}
