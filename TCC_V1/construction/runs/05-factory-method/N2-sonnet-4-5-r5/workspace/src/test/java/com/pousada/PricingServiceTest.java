package com.pousada;

import com.pousada.domain.model.RatePlan;
import com.pousada.domain.model.RoomType;
import com.pousada.domain.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    @Test
    void calculatesHighSeasonPriceCorrectly() {
        PricingService service = new PricingService();

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingService.PriceBreakdown pricing = service.calculate(
            RoomType.STANDARD, checkIn, checkOut, RatePlan.STANDARD
        );

        assertThat(pricing.nights).isEqualTo(2);
        assertThat(pricing.subtotal).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(pricing.discount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pricing.total).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void calculatesNonRefundableDiscountCorrectly() {
        PricingService service = new PricingService();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PriceBreakdown pricing = service.calculate(
            RoomType.DELUXE, checkIn, checkOut, RatePlan.NON_REFUNDABLE
        );

        assertThat(pricing.nights).isEqualTo(2);
        assertThat(pricing.subtotal).isEqualByComparingTo(new BigDecimal("779.80"));
        assertThat(pricing.discount).isEqualByComparingTo(new BigDecimal("116.97"));
        assertThat(pricing.total).isEqualByComparingTo(new BigDecimal("662.83"));
    }
}
