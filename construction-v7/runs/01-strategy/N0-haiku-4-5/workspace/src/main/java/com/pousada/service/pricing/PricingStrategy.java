package com.pousada.service.pricing;

import com.pousada.domain.Room;
import com.pousada.exception.ApiException;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PricingStrategy {
    PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) throws ApiException;

    class PricingResult {
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;
        public final BigDecimal refundPercentage;

        public PricingResult(BigDecimal subtotal, BigDecimal discount, BigDecimal total, BigDecimal refundPercentage) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
            this.refundPercentage = refundPercentage;
        }
    }
}
