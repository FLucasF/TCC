package com.pousada.service;

import com.pousada.model.RatePlan;
import com.pousada.model.Room;
import com.pousada.util.DateUtil;
import com.pousada.util.PriceUtil;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingService {
    public static class PricingResult {
        public int nights;
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;

        public PricingResult(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = DateUtil.countNights(checkIn, checkOut);

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightRate = room.getNightlyRate();
            if (DateUtil.isHighSeason(current)) {
                nightRate = PriceUtil.applyHighSeasonMultiplier(nightRate);
            }
            subtotal = PriceUtil.add(subtotal, nightRate);
            current = current.plusDays(1);
        }

        BigDecimal discount = PriceUtil.applyPercentage(subtotal, ratePlan.getDiscountRate());
        BigDecimal total = PriceUtil.subtract(subtotal, discount);

        return new PricingResult(nights, subtotal, discount, total);
    }
}
