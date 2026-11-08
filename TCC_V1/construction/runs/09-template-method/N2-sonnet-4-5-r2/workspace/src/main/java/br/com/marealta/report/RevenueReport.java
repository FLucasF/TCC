package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RevenueReport extends CsvReport {
    private final YearMonth month;
    private final ReservationRepository reservationRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

    public RevenueReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "revenue";
    }

    @Override
    protected String getPeriod() {
        return month.format(monthFormatter);
    }

    @Override
    protected String buildHeaderLine() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);
        List<String> lines = new ArrayList<>();

        for (Reservation reservation : reservations) {
            BigDecimal netAmount = reservation.getNetAmount();
            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(String.join(",",
                        String.valueOf(reservation.getId()),
                        String.valueOf(reservation.getRoomNumber()),
                        reservation.getCheckIn().format(dateFormatter),
                        reservation.getCheckOut().format(dateFormatter),
                        reservation.getStatus().name(),
                        formatAmount(netAmount)
                ));
            }
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);
        BigDecimal total = reservations.stream()
                .map(Reservation::getNetAmount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return "total," + formatAmount(total);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}
