package com.pousada.rateplan;

import com.pousada.entity.Room;
import com.pousada.util.DateUtil;
import com.pousada.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporatePlan implements RatePlan {
    private static final Map<String, Double> DISCOUNTS = Map.of(
        "TECNOSUL", 12.0,
        "AGROVALE", 18.0,
        "FARMAVIDA", 25.0
    );

    @Override
    public boolean isValid(LocalDate checkIn, LocalDate checkOut, int guests, String companyCode, Room room) {
        return guests >= 1 && checkIn.isBefore(checkOut) && DISCOUNTS.containsKey(companyCode);
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        Double discountPercent = DISCOUNTS.get(companyCode);
        BigDecimal discount = Money.percent(subtotal, discountPercent);
        BigDecimal total = Money.subtract(subtotal, discount);
        return new PriceCalculation(subtotal, discount, total);
    }

    private BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightlyRate = room.nightlyRate;
            if (DateUtil.isHighSeason(current)) {
                nightlyRate = Money.multiply(nightlyRate, BigDecimal.valueOf(1.2));
            }
            total = Money.add(total, nightlyRate);
            current = current.plusDays(1);
        }
        return total;
    }

    @Override
    public BigDecimal getRefundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }

    @Override
    public String getName() {
        return "CORPORATE";
    }
}
