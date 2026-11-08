package com.pousada.report;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RatePlanReport extends Report {
    private final YearMonth month;
    private final List<Reservation> reservations;
    private final DecimalFormat currencyFormatter = new DecimalFormat("0.00");

    public RatePlanReport(YearMonth month) {
        this.month = month;
        this.reservations = ReservationRepository.findByMonthAndStatuses(
                month.getYear(), month.getMonthValue(),
                "CONFIRMED", "CHECKED_IN", "CHECKED_OUT"
        );
    }

    @Override
    protected String buildTitle() {
        return "Pousada Maré Alta,rate-plans," + month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    @Override
    protected String buildHeader() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        List<String> lines = new ArrayList<>();

        for (RatePlan plan : RatePlan.values()) {
            long count = 0;
            BigDecimal amount = BigDecimal.ZERO;

            for (Reservation res : reservations) {
                if (res.ratePlan == plan) {
                    count++;
                    BigDecimal net = res.amountPaid;
                    if (res.refund != null) {
                        net = net.subtract(res.refund);
                    }
                    amount = amount.add(net);
                }
            }

            lines.add(plan.name() + "," + count + "," + currencyFormatter.format(amount));
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        long totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            totalReservations++;
            BigDecimal net = res.amountPaid;
            if (res.refund != null) {
                net = net.subtract(res.refund);
            }
            totalAmount = totalAmount.add(net);
        }

        return "total," + totalReservations + "," + currencyFormatter.format(totalAmount);
    }
}
