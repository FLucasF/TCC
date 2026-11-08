package com.pousada.domain.rateplan;

import com.pousada.domain.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackageRatePlan extends AbstractRatePlan {

    private static final Map<BigDecimal, BigDecimal> PACKAGE_PRICES = Map.of(
        new BigDecimal("249.90"), new BigDecimal("520.00"),
        new BigDecimal("389.90"), new BigDecimal("820.00"),
        new BigDecimal("579.90"), new BigDecimal("1200.00")
    );

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = calculateNights(checkIn, checkOut);

        if (nights != 2) {
            throw new IllegalArgumentException("Pacote de fim de semana requer exatamente 2 noites");
        }

        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new IllegalArgumentException("Pacote de fim de semana deve começar na sexta-feira");
        }

        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Pacote de fim de semana deve terminar no domingo");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(nightlyRate);
        return roundMoney(packagePrice);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        BigDecimal refund = amountPaid.multiply(new BigDecimal("0.5"));
        return roundMoney(refund);
    }
}
