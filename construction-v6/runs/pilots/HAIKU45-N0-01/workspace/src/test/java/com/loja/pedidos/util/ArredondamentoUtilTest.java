package com.loja.pedidos.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArredondamentoUtilTest {
    @Test
    void testArredondamentoMeioParaPar() {
        assertEquals(new BigDecimal("3.00"), ArredondamentoUtil.arredondar(new BigDecimal("2.995")));
        assertEquals(new BigDecimal("2.98"), ArredondamentoUtil.arredondar(new BigDecimal("2.985")));
    }

    @Test
    void testArredondamentoNormal() {
        assertEquals(new BigDecimal("2.99"), ArredondamentoUtil.arredondar(new BigDecimal("2.994")));
        assertEquals(new BigDecimal("3.00"), ArredondamentoUtil.arredondar(new BigDecimal("2.996")));
    }

    @Test
    void testArredondamentoZero() {
        assertEquals(new BigDecimal("0.00"), ArredondamentoUtil.arredondar(null));
    }
}
