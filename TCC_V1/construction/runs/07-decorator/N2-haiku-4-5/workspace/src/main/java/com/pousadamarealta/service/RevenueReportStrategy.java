package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.util.DateUtil;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RevenueReportStrategy implements ReportStrategy {
    @Override
    public String generate(ReservationRepository resRepo, RoomRepository roomRepo, Object param) {
        YearMonth yearMonth = (YearMonth) param;
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> relevant = new ArrayList<>();

        for (Reservation res : resRepo.findAll()) {
            if (res.checkIn.getYear() == yearMonth.getYear() &&
                res.checkIn.getMonthValue() == yearMonth.getMonthValue() &&
                (res.status == ReservationStatus.CONFIRMED ||
                 res.status == ReservationStatus.CHECKED_IN ||
                 res.status == ReservationStatus.CHECKED_OUT ||
                 res.status == ReservationStatus.CANCELLED)) {

                // Only count if there's money (confirmed, checked_in, checked_out, or non-refundable cancelled)
                if (!res.amountPaid.equals(BigDecimal.ZERO) || res.status == ReservationStatus.CANCELLED) {
                    relevant.add(res);
                }
            }
        }

        // Sort by check_in date and then by reservation id
        relevant.sort(Comparator.comparing((Reservation r) -> r.checkIn).thenComparing(r -> r.id));

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : relevant) {
            BigDecimal amount = calculateAmount(res);
            csv.append(res.id).append(",")
               .append(res.roomNumber).append(",")
               .append(DateUtil.formatDateISO(res.checkIn)).append(",")
               .append(DateUtil.formatDateISO(res.checkOut)).append(",")
               .append(res.status.toString()).append(",")
               .append(amount).append("\n");
            total = total.add(amount);
        }

        csv.append("total,,,,").append(total).append("\n");

        return csv.toString();
    }

    private BigDecimal calculateAmount(Reservation res) {
        BigDecimal amount = res.amountPaid;
        if (res.refund != null) {
            amount = amount.subtract(res.refund);
        }
        return amount;
    }
}
