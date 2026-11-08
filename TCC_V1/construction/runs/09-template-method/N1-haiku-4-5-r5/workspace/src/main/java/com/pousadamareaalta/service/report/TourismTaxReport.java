package com.pousadamareaalta.service.report;

import com.pousadamareaalta.database.*;
import com.pousadamareaalta.domain.*;
import com.pousadamareaalta.util.FormatterUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.*;

public class TourismTaxReport implements ReportGenerator {
    private final YearMonth month;
    private static final double TAX_PER_GUEST_PER_NIGHT = 4.50;

    public TourismTaxReport(YearMonth month) {
        this.month = month;
    }

    @Override
    public String generate() throws SQLException {
        List<Reservation> reservations = ReservationRepository.findByMonthStarting(month.getYear(), month.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(month.getYear()).append("-");
        if (month.getMonthValue() < 10) csv.append("0");
        csv.append(month.getMonthValue()).append("\n");

        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        double totalTax = 0.0;
        for (Reservation res : reservations) {
            double tax = calculateTax(res.guests(), res.nights());

            csv.append(res.id()).append(",");
            csv.append(res.roomNumber()).append(",");
            csv.append(FormatterUtil.formatDateISO(res.checkIn())).append(",");
            csv.append(FormatterUtil.formatDateISO(res.checkOut())).append(",");
            csv.append(res.guests()).append(",");
            csv.append(res.nights()).append(",");
            csv.append(FormatterUtil.formatCurrencyCSV(tax)).append("\n");

            totalTax += tax;
        }

        csv.append("total,,,,,").append(FormatterUtil.formatCurrencyCSV(totalTax));

        return csv.toString();
    }

    private double calculateTax(int guests, int nights) {
        BigDecimal tax = new BigDecimal(guests)
            .multiply(new BigDecimal(nights))
            .multiply(new BigDecimal(String.valueOf(TAX_PER_GUEST_PER_NIGHT)))
            .setScale(2, RoundingMode.HALF_EVEN);
        return tax.doubleValue();
    }

    @Override
    public String getReportName() {
        return "tourism-tax";
    }
}
