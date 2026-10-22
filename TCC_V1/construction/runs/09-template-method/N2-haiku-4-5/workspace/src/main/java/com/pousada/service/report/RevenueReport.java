package com.pousada.service.report;

import com.pousada.domain.Reservation;
import com.pousada.repo.ReservationRepository;
import com.pousada.util.DateUtil;
import com.pousada.util.Money;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RevenueReport extends Report {
    private final ReservationRepository reservationRepo;
    private final int year;
    private final int month;

    public RevenueReport(ReservationRepository reservationRepo, YearMonth yearMonth) throws Exception {
        super("revenue", yearMonth.toString());
        this.reservationRepo = reservationRepo;
        this.year = yearMonth.getYear();
        this.month = yearMonth.getMonthValue();
    }

    @Override
    protected String getColumnHeader() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> getDataRows() {
        List<String> rows = new ArrayList<>();
        try {
            List<Reservation> reservations = reservationRepo.findByMonthAndPaid(year, month);
            for (Reservation res : reservations) {
                rows.add(formatRow(res));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    @Override
    protected String getTotalLine() {
        try {
            List<Reservation> reservations = reservationRepo.findByMonthAndPaid(year, month);
            BigDecimal total = BigDecimal.ZERO;
            for (Reservation res : reservations) {
                BigDecimal net = Money.subtract(res.getAmountPaid(), res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO);
                total = Money.add(total, net);
            }
            return "total," + formatMoney(total);
        } catch (Exception e) {
            return "total,0.00";
        }
    }

    private String formatRow(Reservation reservation) {
        BigDecimal net = Money.subtract(reservation.getAmountPaid(), reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO);
        return reservation.getId() + "," +
               reservation.getRoomNumber() + "," +
               DateUtil.formatDateForCsv(reservation.getCheckIn()) + "," +
               DateUtil.formatDateForCsv(reservation.getCheckOut()) + "," +
               reservation.getStatus().getDisplayName() + "," +
               formatMoney(net);
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
