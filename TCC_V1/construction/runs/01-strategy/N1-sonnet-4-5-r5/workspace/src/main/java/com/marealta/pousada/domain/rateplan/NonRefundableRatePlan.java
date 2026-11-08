package com.marealta.pousada.domain.rateplan;

import com.marealta.pousada.domain.model.RoomType;
import com.marealta.pousada.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundableRatePlan extends AbstractRatePlan {

    @Override
    public String getName() {
        return "NON_REFUNDABLE";
    }

    @Override
    public BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, RoomType roomType) {
        return calculateStandardSubtotal(checkIn, checkOut, roomType);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return MoneyUtil.percentage(subtotal, new BigDecimal("15"));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
