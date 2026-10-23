package com.pousadamarealta.reports;

import com.pousadamarealta.database.ReservationDao;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport {
    private final ReservationDao reservationDao;

    public RevenueReport(ReservationDao reservationDao) {
        this.reservationDao = reservationDao;
    }

    public String generate(YearMonth month) {
        List<Reservation> reservations = reservationDao.findReservationsStartingInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            csv.append(String.format("%d,%s,%s,%s,%s,%.2f\n",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getStatus().name(),
                revenue
            ));

            total = total.add(revenue);
        }

        csv.append(String.format("total,%.2f\n", total));

        return csv.toString();
    }
}
