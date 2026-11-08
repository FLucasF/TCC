package pousada.domain.rateplan;

import pousada.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayRatePlan extends AbstractRatePlan {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    private static final int MINIMUM_NIGHTS = 7;

    @Override
    public String name() {
        return "LONG_STAY";
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        if (countNights(checkIn, checkOut) < MINIMUM_NIGHTS) {
            throw new IllegalArgumentException("Long stay requires at least " + MINIMUM_NIGHTS + " nights");
        }
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
        return amountPaid;
    }
}
