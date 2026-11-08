package com.marealta.pousada.domain.rateplan;

import com.marealta.pousada.domain.model.RoomType;
import com.marealta.pousada.exception.InvalidRequestException;
import com.marealta.pousada.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayRatePlan extends AbstractRatePlan {

    @Override
    public String getName() {
        return "LONG_STAY";
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        long nights = countNights(checkIn, checkOut);

        if (nights < 7) {
            throw new InvalidRequestException("Long stay requires at least 7 nights");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, RoomType roomType) {
        return calculateStandardSubtotal(checkIn, checkOut, roomType);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return MoneyUtil.percentage(subtotal, new BigDecimal("10"));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
