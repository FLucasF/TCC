package com.pousada.service.report;

import com.pousada.domain.Reservation;
import com.pousada.repo.ReservationRepository;
import com.pousada.util.DateUtil;
import com.pousada.util.Money;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class TourismTaxReport extends Report {
    private final ReservationRepository reservationRepo;
    private final int year;
    private final int month;
    private final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    public TourismTaxReport(ReservationRepository reservationRepo, YearMonth yearMonth) throws Exception {
        super("tourism-tax", yearMonth.toString());
        this.reservationRepo = reservationRepo;
        this.year = yearMonth.getYear();
        this.month = yearMonth.getMonthValue();
    }

    @Override
    protected String getColumnHeader() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
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
                BigDecimal tax = calculateTax(res);
                total = Money.add(total, tax);
            }
            return "total," + formatMoney(total);
        } catch (Exception e) {
            return "total,0.00";
        }
    }

    private String formatRow(Reservation reservation) {
        BigDecimal tax = calculateTax(reservation);
        return reservation.getId() + "," +
               reservation.getRoomNumber() + "," +
               DateUtil.formatDateForCsv(reservation.getCheckIn()) + "," +
               DateUtil.formatDateForCsv(reservation.getCheckOut()) + "," +
               reservation.getGuests() + "," +
               reservation.getNights() + "," +
               formatMoney(tax);
    }

    private BigDecimal calculateTax(Reservation reservation) {
        BigDecimal guestNights = new BigDecimal(reservation.getGuests() * reservation.getNights());
        return Money.multiply(guestNights, TAX_PER_GUEST_PER_NIGHT);
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
