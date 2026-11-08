package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** The tourism tax the city charges on the paid reservations that start in a month: a value per guest per night. */
public final class TourismTaxReport extends Report<YearMonth> {

    private final ReservationStore store;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestNight) {
        this.store = store;
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override
    protected String name() {
        return "tourism-tax";
    }

    @Override
    protected List<String> header() {
        return List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax");
    }

    @Override
    protected Table table(YearMonth month) {
        List<Reservation> paid = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            if (reservation.isPaid() && MonthlyRevenue.startsIn(reservation, month)) {
                paid.add(reservation);
            }
        }
        paid.sort(MonthlyRevenue.ORDER);

        List<List<String>> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : paid) {
            int nights = reservation.price().nights();
            BigDecimal tax = taxPerGuestNight.multiply(BigDecimal.valueOf((long) reservation.guests() * nights))
                    .setScale(2);
            lines.add(List.of(
                    String.valueOf(reservation.id()),
                    String.valueOf(reservation.roomNumber()),
                    reservation.checkIn().toString(),
                    reservation.checkOut().toString(),
                    String.valueOf(reservation.guests()),
                    String.valueOf(nights),
                    tax.toPlainString()));
            total = total.add(tax);
        }
        return new Table(lines, List.of("total", total.toPlainString()));
    }
}
