package br.com.maralta.domain.rateplan;

import br.com.maralta.domain.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class LongStayRatePlan implements RatePlan {

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, RoomType roomType, String companyCode) {
        if (nights < 7) {
            throw new IllegalArgumentException("Long stay requires at least 7 nights");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, int nights, RoomType roomType) {
        BigDecimal baseRate = roomType.getNightlyRate();
        BigDecimal subtotal = BigDecimal.ZERO;

        LocalDate currentDate = checkIn;
        for (int i = 0; i < nights; i++) {
            BigDecimal nightRate = baseRate;
            if (isHighSeason(currentDate)) {
                nightRate = nightRate.multiply(new BigDecimal("1.20"));
            }
            nightRate = nightRate.setScale(2, RoundingMode.HALF_EVEN);
            subtotal = subtotal.add(nightRate);
            currentDate = currentDate.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        BigDecimal discount = subtotal.multiply(new BigDecimal("0.10"));
        return discount.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
