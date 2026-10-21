package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The tourism tax the town charges, from the paid reservations that start in a given month,
 * for the town hall (CSV).
 */
public final class TourismTaxReport {

    private final ReservationStore store;
    private final BigDecimal taxPerGuestPerNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestPerNight) {
        this.store = store;
        this.taxPerGuestPerNight = taxPerGuestPerNight;
    }

    public String generate(YearMonth month) {
        List<Reservation> paidInMonth = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean isPaid = reservation.isPaid();
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (isPaid && startsInMonth) {
                paidInMonth.add(reservation);
            }
        }
        paidInMonth.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        CsvReport report = new CsvReport("tourism-tax", month.toString(),
                List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax"));
        BigDecimal totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : paidInMonth) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = taxPerGuestPerNight
                    .multiply(BigDecimal.valueOf(reservation.guests()))
                    .multiply(BigDecimal.valueOf(nights));
            report.addRow(
                    String.valueOf(reservation.id()),
                    String.valueOf(reservation.roomNumber()),
                    reservation.checkIn().toString(),
                    reservation.checkOut().toString(),
                    String.valueOf(reservation.guests()),
                    String.valueOf(nights),
                    tax.toPlainString());
            totalTax = totalTax.add(tax);
        }
        report.setTotal("total", totalTax.toPlainString());
        return report.render();
    }
}
