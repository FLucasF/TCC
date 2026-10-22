package br.com.marealta;

import br.com.marealta.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {
    @Test
    void formatsBrl() {
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.brl(Money.ZERO));
        assertEquals("R$ 1.234.567,80", Money.brl(new BigDecimal("1234567.8")));
    }

    @Test
    void halfEven() {
        assertEquals(new BigDecimal("0.12"), Money.round(new BigDecimal("0.125")));
        assertEquals(new BigDecimal("0.14"), Money.round(new BigDecimal("0.135")));
    }
}
