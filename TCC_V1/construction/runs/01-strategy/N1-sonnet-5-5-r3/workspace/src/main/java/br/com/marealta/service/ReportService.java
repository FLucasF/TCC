package br.com.marealta.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.Rooms;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.ReservationRepository;

public class ReportService {
    private final Database database;
    private final ReservationRepository reservations = new ReservationRepository();

    public ReportService(Database database) {
        this.database = database;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = database.tx(connection -> reservations.findPaidOccupying(connection, night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation reservation : occupying) {
            Room room = Rooms.find(reservation.roomNumber()).orElseThrow();
            csv.append(String.join(",", String.valueOf(room.number()), room.type().name(),
                    String.valueOf(reservation.id()), reservation.checkIn().toString(),
                    reservation.checkOut().toString())).append('\n');
        }
        csv.append("total,").append(occupying.size()).append('/').append(Rooms.ALL.size()).append('\n');
        return csv.toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = database.tx(connection ->
                reservations.findWithRevenueStartingBetween(connection, month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation reservation : kept) {
            BigDecimal amount = reservation.amountPaid().subtract(
                    reservation.refund() == null ? Money.ZERO : reservation.refund());
            total = total.add(amount);
            csv.append(String.join(",", String.valueOf(reservation.id()), String.valueOf(reservation.roomNumber()),
                    reservation.checkIn().toString(), reservation.checkOut().toString(),
                    reservation.status().name(), amount.toPlainString())).append('\n');
        }
        csv.append("total,").append(total.toPlainString()).append('\n');
        return csv.toString();
    }
}
