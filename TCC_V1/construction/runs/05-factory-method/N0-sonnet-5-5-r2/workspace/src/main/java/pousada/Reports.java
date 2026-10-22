package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Reports {
    private Reports() {}

    public static String occupancy(List<Reservation> occupying) {
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.roomNumber()).append(',').append(r.room().type()).append(',').append(r.id()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        return csv.append("total,").append(occupying.size()).append('/').append(Room.ALL.size()).append('\n')
                .toString();
    }

    public static String revenue(List<Reservation> reservations) {
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal sum = Money.ZERO;
        for (Reservation r : reservations) {
            BigDecimal kept = r.amountPaid().subtract(r.refund() == null ? Money.ZERO : r.refund());
            sum = sum.add(kept);
            csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',').append(kept.toPlainString())
                    .append('\n');
        }
        return csv.append("total,").append(sum.toPlainString()).append('\n').toString();
    }
}
