package com.pousadamarealta.report;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.util.DateUtils;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.util.List;

public class RevenueReport extends BaseReport {
    private final ReservationRepository reservationRepository;

    public RevenueReport() {
        this.reservationRepository = new ReservationRepository();
    }

    public String generate(int year, int month) {
        addTitleLine("revenue", String.format("%04d-%02d", year, month));
        addHeaderLine("reservation", "room", "check_in", "check_out", "status", "amount");

        List<Reservation> reservations = reservationRepository.findByMonth(year, month);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal netAmount = r.getAmountPaid();
            if (r.getRefund() != null) {
                netAmount = netAmount.subtract(r.getRefund());
            }

            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                addDataLine(
                    r.getId().toString(),
                    r.getRoomNumber(),
                    DateUtils.formatISO(r.getCheckIn()),
                    DateUtils.formatISO(r.getCheckOut()),
                    r.getStatus().name(),
                    MoneyUtils.formatCSV(netAmount)
                );
                totalAmount = totalAmount.add(netAmount);
            }
        }

        addDataLine("total", MoneyUtils.formatCSV(totalAmount));

        return super.generate();
    }
}
