package com.loja.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldRoundUsingBankersRounding() {
        Money m1 = new Money("2.995");
        assertEquals(new BigDecimal("3.00"), m1.value());

        Money m2 = new Money("2.985");
        assertEquals(new BigDecimal("2.98"), m2.value());
    }

    @Test
    void shouldAddMoney() {
        Money m1 = new Money("10.50");
        Money m2 = new Money("5.25");
        Money result = m1.add(m2);
        assertEquals(new BigDecimal("15.75"), result.value());
    }

    @Test
    void shouldCalculatePercentage() {
        Money m = new Money("100.00");
        Money result = m.percentage(BigDecimal.valueOf(10));
        assertEquals(new BigDecimal("10.00"), result.value());
    }
}
