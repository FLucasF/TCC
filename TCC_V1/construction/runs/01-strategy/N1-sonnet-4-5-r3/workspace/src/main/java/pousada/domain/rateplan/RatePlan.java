package pousada.domain.rateplan;

import pousada.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {
    String name();

    void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode);

    PricingResult calculatePricing(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
