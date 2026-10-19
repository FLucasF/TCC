package com.pousada.service.pricing;

import com.pousada.domain.Room;
import com.pousada.exception.ApiException;
import com.pousada.util.DateHelper;
import com.pousada.util.MoneyRounder;
import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) throws ApiException {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = room.getNightlyRate();

            if (DateHelper.isHighSeason(current)) {
                nightPrice = MoneyRounder.multiply(nightPrice, BigDecimal.valueOf(1.2));
            }

            subtotal = MoneyRounder.add(subtotal, nightPrice);
            current = current.plusDays(1);
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal;

        return new PricingResult(subtotal, discount, total, BigDecimal.valueOf(100));
    }
}
