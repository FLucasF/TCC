package com.pousada.domain.rateplan;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {
    void validate(LocalDate checkIn, LocalDate checkOut, String companyCode);

    BigDecimal calculateSubtotal(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateDiscount(BigDecimal subtotal, BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
