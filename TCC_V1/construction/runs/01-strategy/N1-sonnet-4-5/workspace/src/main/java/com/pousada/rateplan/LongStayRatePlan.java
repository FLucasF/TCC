package com.pousada.rateplan;

import com.pousada.exception.InvalidRequestException;
import com.pousada.model.Room;
import com.pousada.model.RatePlanType;
import com.pousada.util.DateUtil;
import com.pousada.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LongStayRatePlan implements RatePlan {

    @Override
    public RatePlanType getType() {
        return RatePlanType.LONG_STAY;
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        if (nights < 7) {
            throw new InvalidRequestException("Estadia longa requer no mínimo 7 noites");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightRate = room.getNightlyRate();

            if (DateUtil.isHighSeason(current)) {
                nightRate = MoneyUtil.round(nightRate.multiply(new BigDecimal("1.20")));
            }

            subtotal = subtotal.add(nightRate);
            current = current.plusDays(1);
        }

        return MoneyUtil.round(subtotal);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return MoneyUtil.round(subtotal.multiply(new BigDecimal("0.10")));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
