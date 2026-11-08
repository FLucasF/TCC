package com.pousadamareaalta.service.report;

import com.pousadamareaalta.database.*;
import com.pousadamareaalta.domain.*;
import com.pousadamareaalta.util.FormatterUtil;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.*;

public class RevenueReport implements ReportGenerator {
    private final YearMonth month;

    public RevenueReport(YearMonth month) {
        this.month = month;
    }

    @Override
    public String generate() throws SQLException {
        List<Reservation> reservations = ReservationRepository.findByMonthStarting(month.getYear(), month.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(month.getYear()).append("-");
        if (month.getMonthValue() < 10) csv.append("0");
        csv.append(month.getMonthValue()).append("\n");

        csv.append("reservation,room,check_in,check_out,status,amount\n");

        double totalAmount = 0.0;
        for (Reservation res : reservations) {
            double amount = res.amountPaid();
            if (res.refund() != null) {
                amount -= res.refund();
            }

            csv.append(res.id()).append(",");
            csv.append(res.roomNumber()).append(",");
            csv.append(FormatterUtil.formatDateISO(res.checkIn())).append(",");
            csv.append(FormatterUtil.formatDateISO(res.checkOut())).append(",");
            csv.append(res.status()).append(",");
            csv.append(FormatterUtil.formatCurrencyCSV(amount)).append("\n");

            totalAmount += amount;
        }

        csv.append("total,,,,").append(FormatterUtil.formatCurrencyCSV(totalAmount));

        return csv.toString();
    }

    @Override
    public String getReportName() {
        return "revenue";
    }
}
