package com.pousadamareaalta.pricing;

import com.pousadamareaalta.model.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingTest {

    @Test
    void shouldCalculateCorporateTecnosul() {
        CorporateRatePlan plan = new CorporateRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingResult result = plan.calculatePrice(RoomType.DELUXE, checkIn, checkOut, 2, "TECNOSUL");

        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("779.80"));
        assertThat(result.getDiscount()).isEqualTo(new BigDecimal("93.58"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("686.22"));
    }

    @Test
    void shouldCalculateWeekendPackage() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        plan.validate(checkIn, checkOut, 2, null);
        PricingResult result = plan.calculatePrice(RoomType.DELUXE, checkIn, checkOut, 2, null);

        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("820.00"));
        assertThat(result.getDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("820.00"));
    }

    @Test
    void shouldCalculateWeekendPackageCancellationRefund() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();
        BigDecimal amountPaid = new BigDecimal("820.00");

        BigDecimal refund = plan.calculateRefund(amountPaid);

        assertThat(refund).isEqualTo(new BigDecimal("410.00"));
    }

    @Test
    void shouldCalculateLongStay() {
        LongStayRatePlan plan = new LongStayRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        PricingResult result = plan.calculatePrice(RoomType.STANDARD, checkIn, checkOut, 7, null);

        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("1749.30"));
        assertThat(result.getDiscount()).isEqualTo(new BigDecimal("174.93"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("1574.37"));
    }

    @Test
    void shouldCalculateHighSeasonStandard() {
        StandardRatePlan plan = new StandardRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingResult result = plan.calculatePrice(RoomType.STANDARD, checkIn, checkOut, 2, null);

        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("599.76"));
        assertThat(result.getDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("599.76"));
    }

    @Test
    void shouldApplyHighSeasonMultiplier() {
        assertThat(SeasonUtil.isHighSeason(LocalDate.of(2027, 12, 14))).isFalse();
        assertThat(SeasonUtil.isHighSeason(LocalDate.of(2027, 12, 15))).isTrue();
        assertThat(SeasonUtil.isHighSeason(LocalDate.of(2027, 1, 15))).isTrue();
        assertThat(SeasonUtil.isHighSeason(LocalDate.of(2027, 2, 28))).isTrue();
        assertThat(SeasonUtil.isHighSeason(LocalDate.of(2027, 3, 1))).isFalse();
        assertThat(SeasonUtil.isHighSeason(LocalDate.of(2027, 7, 1))).isTrue();
        assertThat(SeasonUtil.isHighSeason(LocalDate.of(2027, 7, 31))).isTrue();
        assertThat(SeasonUtil.isHighSeason(LocalDate.of(2027, 8, 1))).isFalse();
    }
}
