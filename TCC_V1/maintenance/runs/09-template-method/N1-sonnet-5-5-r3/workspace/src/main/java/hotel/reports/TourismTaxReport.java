package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** The tourism tax the city charges on the paid reservations that start in a given month (CSV). */
public final class TourismTaxReport extends CsvReport<YearMonth, Reservation> {

    private final MonthlyReservations reservations;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestNight) {
        super("tourism-tax");
        this.reservations = new MonthlyReservations(store);
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<Reservation> rows(YearMonth month) {
        return reservations.paid(month);
    }

    @Override
    protected String line(Reservation reservation) {
        return reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                + reservation.checkOut() + "," + reservation.guests() + "," + nights(reservation) + ","
                + tax(reservation).toPlainString();
    }

    @Override
    protected String total(List<Reservation> paid) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : paid) {
            total = total.add(tax(reservation));
        }
        return "total," + total.toPlainString();
    }

    private static long nights(Reservation reservation) {
        return ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
    }

    private BigDecimal tax(Reservation reservation) {
        return taxPerGuestNight
                .multiply(BigDecimal.valueOf(reservation.guests()))
                .multiply(BigDecimal.valueOf(nights(reservation)))
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
