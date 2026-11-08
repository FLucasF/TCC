package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The city's tourism tax on the paid reservations that start in a given month, for the city hall (CSV). */
public final class TourismTaxReport {

    private final ReservationStore store;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestNight) {
        this.store = store;
        this.taxPerGuestNight = taxPerGuestNight;
    }

    public String generate(YearMonth month) {
        List<Reservation> taxed = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            if (reservation.isPaid() && YearMonth.from(reservation.checkIn()).equals(month)) {
                taxed.add(reservation);
            }
        }
        taxed.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        CsvReport csv = new CsvReport("tourism-tax", month, "reservation,room,check_in,check_out,guests,nights,tax");
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : taxed) {
            int nights = reservation.price().nights();
            BigDecimal tax = taxPerGuestNight.multiply(BigDecimal.valueOf((long) reservation.guests() * nights))
                    .setScale(2, RoundingMode.HALF_EVEN);
            csv.line(reservation.id(), reservation.roomNumber(), reservation.checkIn(), reservation.checkOut(),
                    reservation.guests(), nights, tax.toPlainString());
            total = total.add(tax);
        }
        csv.line("total", total.toPlainString());
        return csv.build();
    }
}
