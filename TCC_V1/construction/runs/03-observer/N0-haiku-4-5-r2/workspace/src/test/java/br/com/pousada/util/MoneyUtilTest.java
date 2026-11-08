package br.com.pousada.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MoneyUtilTest {

    @Test
    public void testRound() {
        // Teste arredondamento "meio para o par"
        assertEquals(249.90, MoneyUtil.round(249.90), 0.001);
        assertEquals(249.89, MoneyUtil.round(249.894), 0.001);
        assertEquals(249.90, MoneyUtil.round(249.896), 0.001);
    }

    @Test
    public void testHighSeasonIncrease() {
        // 249.90 com 20% de aumento = 299.88
        double highSeason = MoneyUtil.applyPercentage(249.90, 20);
        assertEquals(299.88, MoneyUtil.round(highSeason));
    }

    @Test
    public void testNonRefundableDiscount() {
        // 599.76 com 15% de desconto = 89.964, arredonda para 89.96
        double discount = MoneyUtil.calculateDiscount(599.76, 15);
        assertEquals(89.96, MoneyUtil.round(discount));
    }

    @Test
    public void testMoneyFormat() {
        String formatted = MoneyUtil.formatBr(1234.56);
        assertTrue(formatted.contains("R$"));
        assertTrue(formatted.contains("1"));
    }
}
