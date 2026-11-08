package pousada.domain.rateplan;

import pousada.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundableRatePlan extends AbstractRatePlan {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    @Override
    public String name() {
        return "NON_REFUNDABLE";
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
    }

    @Override
    public PricingResult calculatePricing(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        BigDecimal subtotal = calculateSubtotalForNights(checkIn, checkOut, roomType);
        BigDecimal discount = round(subtotal.multiply(DISCOUNT_RATE));
        BigDecimal total = round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
