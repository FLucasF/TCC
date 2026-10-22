package br.com.marealta.domain;

import br.com.marealta.domain.rateplan.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatePlanTest {

    @Test
    void standardPlanJulyHighSeason() {
        RatePlan plan = new StandardRatePlan();
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );

        Money subtotal = plan.calculateSubtotal(RoomType.STANDARD, dateRange);
        assertThat(subtotal.amount()).isEqualByComparingTo("599.76");

        Money discount = plan.calculateDiscount(subtotal);
        assertThat(discount.amount()).isEqualByComparingTo("0.00");

        Money total = subtotal.subtract(discount);
        assertThat(total.amount()).isEqualByComparingTo("599.76");
    }

    @Test
    void corporateTecnosulDeluxe() {
        CorporateRatePlan plan = new CorporateRatePlan();
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12)
        );

        Money subtotal = plan.calculateSubtotal(RoomType.DELUXE, dateRange);
        assertThat(subtotal.amount()).isEqualByComparingTo("779.80");

        Money discount = plan.calculateDiscount(subtotal, "TECNOSUL");
        assertThat(discount.amount()).isEqualByComparingTo("93.58");

        Money total = subtotal.subtract(discount);
        assertThat(total.amount()).isEqualByComparingTo("686.22");
    }

    @Test
    void weekendPackageDeluxe() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14)
        );

        Money subtotal = plan.calculateSubtotal(RoomType.DELUXE, dateRange);
        assertThat(subtotal.amount()).isEqualByComparingTo("820.00");

        Money discount = plan.calculateDiscount(subtotal);
        assertThat(discount.amount()).isEqualByComparingTo("0.00");

        Money refund = plan.calculateRefund(Money.of("820.00"));
        assertThat(refund.amount()).isEqualByComparingTo("410.00");
    }

    @Test
    void longStayStandard() {
        LongStayRatePlan plan = new LongStayRatePlan();
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 3, 8)
        );

        Money subtotal = plan.calculateSubtotal(RoomType.STANDARD, dateRange);
        assertThat(subtotal.amount()).isEqualByComparingTo("1749.30");

        Money discount = plan.calculateDiscount(subtotal);
        assertThat(discount.amount()).isEqualByComparingTo("174.93");

        Money total = subtotal.subtract(discount);
        assertThat(total.amount()).isEqualByComparingTo("1574.37");
    }

    @Test
    void weekendPackageRequiresFridayToSunday() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 3, 13),
            LocalDate.of(2027, 3, 15)
        );

        assertThatThrownBy(() -> plan.validate(RoomType.DELUXE, dateRange, 2, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void longStayRequires7Nights() {
        LongStayRatePlan plan = new LongStayRatePlan();
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 3, 7)
        );

        assertThatThrownBy(() -> plan.validate(RoomType.STANDARD, dateRange, 1, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void corporateRequiresValidCompanyCode() {
        CorporateRatePlan plan = new CorporateRatePlan();
        DateRange dateRange = new DateRange(
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12)
        );

        assertThatThrownBy(() -> plan.validate(RoomType.DELUXE, dateRange, 2, null))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> plan.validate(RoomType.DELUXE, dateRange, 2, "INVALID"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
