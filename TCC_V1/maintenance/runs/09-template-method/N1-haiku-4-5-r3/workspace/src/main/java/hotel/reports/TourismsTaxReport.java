package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TourismsTaxReport extends CsvReport {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;

    public TourismsTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> paidReservations = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                paidReservations.add(reservation);
            }
        }
        paidReservations.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        StringBuilder rows = new StringBuilder();
        BigDecimal totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : paidReservations) {
            long nights = reservation.checkOut().toEpochDay() - reservation.checkIn().toEpochDay();
            BigDecimal reservationTax = TAX_PER_GUEST_PER_NIGHT.multiply(BigDecimal.valueOf(reservation.guests() * nights)).setScale(2, java.math.RoundingMode.HALF_EVEN);
            rows.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.guests()).append(',')
                    .append(nights).append(',')
                    .append(reservationTax.toPlainString()).append('\n');
            totalTax = totalTax.add(reservationTax);
        }
        String total = "total," + totalTax.toPlainString() + "\n";

        return formatCsv("tourism-tax", month.toString(), "reservation,room,check_in,check_out,guests,nights,tax", rows.toString(), total);
    }
}
