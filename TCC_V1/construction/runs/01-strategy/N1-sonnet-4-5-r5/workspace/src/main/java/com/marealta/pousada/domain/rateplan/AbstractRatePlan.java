package com.marealta.pousada.domain.rateplan;

import com.marealta.pousada.domain.model.RoomType;
import com.marealta.pousada.util.DateUtil;
import com.marealta.pousada.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

public abstract class AbstractRatePlan implements RatePlan {

    protected BigDecimal calculateStandardSubtotal(LocalDate checkIn, LocalDate checkOut, RoomType roomType) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightRate = roomType.getNightlyRate();

            if (DateUtil.isHighSeason(currentDate)) {
                nightRate = MoneyUtil.round(nightRate.multiply(new BigDecimal("1.20")));
            } else {
                nightRate = MoneyUtil.round(nightRate);
            }

            subtotal = subtotal.add(nightRate);
            currentDate = currentDate.plusDays(1);
        }

        return MoneyUtil.round(subtotal);
    }

    protected long countNights(LocalDate checkIn, LocalDate checkOut) {
        return DateUtil.countNights(checkIn, checkOut);
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
    }
}
