package br.com.mareaalta;

import br.com.mareaalta.util.Money;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MoneyTest {
    @Test
    public void testBankersRounding() {
        assertEquals(249.90, Money.round(249.898), 0.01);
        assertEquals(249.90, Money.round(249.895), 0.01);
        assertEquals(249.88, Money.round(249.882), 0.01);
        assertEquals(299.88, Money.round(299.88), 0.01);
    }

    @Test
    public void testFormat() {
        assertEquals("249.90", Money.format(249.90));
        assertEquals("1234.56", Money.format(1234.56));
    }
}
