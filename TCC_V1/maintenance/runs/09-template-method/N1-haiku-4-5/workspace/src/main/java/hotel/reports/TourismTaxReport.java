package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The tourism tax owed to the city: R$ 4.50 per guest per night, for paid reservations that start
 * in a given month.
 */
public final class TourismTaxReport {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");
    private final ReservationStore store;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }
        withRevenue.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(month).append('\n');
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");
        BigDecimal totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : withRevenue) {
            int nights = reservation.price().nights();
            int guests = reservation.guests();
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT.multiply(new BigDecimal(guests * nights));
            tax = tax.setScale(2, java.math.RoundingMode.HALF_EVEN);
            csv.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(guests).append(',')
                    .append(nights).append(',')
                    .append(tax.toPlainString()).append('\n');
            totalTax = totalTax.add(tax);
        }
        csv.append("total,").append(totalTax.toPlainString()).append('\n');
        return csv.toString();
    }
}
