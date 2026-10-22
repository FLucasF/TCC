package com.pousada;

import com.pousada.utils.Money;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

public class MoneyTest {
    @Test
    public void roundToHalfEven() {
        assertEquals(new BigDecimal("299.88"), Money.round(new BigDecimal("299.880")));
        assertEquals(new BigDecimal("100.00"), Money.round(new BigDecimal("99.995")));
        assertEquals(new BigDecimal("100.00"), Money.round(new BigDecimal("100.005")));
    }

    @Test
    public void addMoney() {
        BigDecimal result = Money.add(new BigDecimal("249.90"), new BigDecimal("249.90"));
        assertEquals(new BigDecimal("499.80"), result);
    }

    @Test
    public void multiply() {
        BigDecimal result = Money.multiply(new BigDecimal("249.90"), new BigDecimal("1.20"));
        assertEquals(new BigDecimal("299.88"), result);
    }

    @Test
    public void percentage() {
        BigDecimal result = Money.percentage(new BigDecimal("599.76"), 15);
        assertEquals(new BigDecimal("89.96"), result);
    }
}
