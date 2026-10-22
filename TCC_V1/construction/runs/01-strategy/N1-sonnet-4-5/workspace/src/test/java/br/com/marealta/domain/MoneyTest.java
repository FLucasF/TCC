package br.com.marealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void shouldRoundToHalfEven() {
        Money money1 = Money.of("10.125");
        assertThat(money1.amount()).isEqualByComparingTo("10.12");

        Money money2 = Money.of("10.135");
        assertThat(money2.amount()).isEqualByComparingTo("10.14");

        Money money3 = Money.of("10.145");
        assertThat(money3.amount()).isEqualByComparingTo("10.14");

        Money money4 = Money.of("10.155");
        assertThat(money4.amount()).isEqualByComparingTo("10.16");
    }

    @Test
    void shouldCalculateDiscount() {
        Money base = Money.of("100.00");
        Money discount = base.discount(BigDecimal.valueOf(15));
        assertThat(discount.amount()).isEqualByComparingTo("15.00");

        Money afterDiscount = base.applyDiscount(BigDecimal.valueOf(15));
        assertThat(afterDiscount.amount()).isEqualByComparingTo("85.00");
    }
}
