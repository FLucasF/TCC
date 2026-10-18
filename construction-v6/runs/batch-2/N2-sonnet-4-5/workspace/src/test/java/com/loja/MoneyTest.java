package com.loja;

import com.loja.domain.entities.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void testRoundingHalfToEven() {
        assertEquals(Money.of(3.00), Money.of(2.995));
        assertEquals(Money.of(2.98), Money.of(2.985));
    }

    @Test
    void testMoneyOperations() {
        Money a = Money.of(10.00);
        Money b = Money.of(3.00);

        assertEquals(Money.of(13.00), a.add(b));
        assertEquals(Money.of(7.00), a.subtract(b));
        assertEquals(Money.of(30.00), a.multiply(3));
    }
}
