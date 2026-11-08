package br.com.maralta.domain.rateplan;

import br.com.maralta.domain.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlan {

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, RoomType roomType, String companyCode) {
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
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
