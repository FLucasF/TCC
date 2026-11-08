package com.pousada.marealta.service;

import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.persistence.ReservationDao;
import com.pousada.marealta.persistence.RoomDao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final ReservationDao reservationDao;
    private final RoomDao roomDao;

    public ReportService(ReservationDao reservationDao, RoomDao roomDao) {
        this.reservationDao = reservationDao;
        this.roomDao = roomDao;
    }

    public String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> reservations = reservationDao.findByDateAndStatus(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : reservations) {
            String type = roomDao.findByNumber(res.getRoomNumber()).getType().name();
            csv.append(String.format("%d,%s,%d,%s,%s\n",
                res.getRoomNumber(),
                type,
                res.getId(),
                formatDate(res.getCheckIn()),
                formatDate(res.getCheckOut())
            ));
        }

        int occupiedRooms = (int) reservations.stream()
            .map(Reservation::getRoomNumber)
            .distinct()
            .count();

        csv.append(String.format("total,%d/5\n", occupiedRooms));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) throws Exception {
        List<Reservation> reservations = reservationDao.findByMonthAndStatus(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(String.format("%d,%d,%s,%s,%s,%s\n",
                res.getId(),
                res.getRoomNumber(),
                formatDate(res.getCheckIn()),
                formatDate(res.getCheckOut()),
                res.getStatus().name(),
                formatMoney(amount)
            ));

            total = total.add(amount);
        }

        csv.append(String.format("total,%s\n", formatMoney(total)));

        return csv.toString();
    }

    private String formatDate(LocalDate date) {
        return String.format("%d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
