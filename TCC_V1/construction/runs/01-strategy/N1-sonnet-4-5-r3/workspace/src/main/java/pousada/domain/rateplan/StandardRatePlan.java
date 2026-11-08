package pousada.domain.rateplan;

import pousada.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan extends AbstractRatePlan {

    @Override
    public String name() {
        return "STANDARD";
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
    }

    @Override
    public PricingResult calculatePricing(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        BigDecimal subtotal = calculateSubtotalForNights(checkIn, checkOut, roomType);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal;

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
