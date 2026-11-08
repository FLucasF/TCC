package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.util.MathUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = BigDecimal.valueOf(1.2);
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = BigDecimal.valueOf(0.15);

    public static PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut,
                                               Reservation.RatePlan ratePlan) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal nightly = room.getNightlyRate();
        if (isHighSeason(checkIn, checkOut)) {
            nightly = MathUtil.roundHalfEven(nightly.multiply(HIGH_SEASON_MULTIPLIER));
        }

        BigDecimal subtotal = MathUtil.roundHalfEven(nightly.multiply(BigDecimal.valueOf(nights)));

        BigDecimal discount = BigDecimal.ZERO;
        if (ratePlan == Reservation.RatePlan.NON_REFUNDABLE) {
            discount = MathUtil.roundHalfEven(subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE));
        }

        BigDecimal total = MathUtil.roundHalfEven(subtotal.subtract(discount));

        return new PricingResult(nights, subtotal, discount, total);
    }

    private static boolean isHighSeason(LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            int month = current.getMonthValue();
            int dayOfMonth = current.getDayOfMonth();

            if ((month == 7) ||
                (month == 12 && dayOfMonth >= 15) ||
                (month == 1) ||
                (month == 2)) {
                return true;
            }

            current = current.plusDays(1);
        }

        return false;
    }

    public static class PricingResult {
        public final int nights;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public PricingResult(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }
}
