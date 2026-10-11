package br.com.loja.checkout;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DinheiroTest {

    @Test
    void arredondaMeioParaOPar() {
        assertEquals(new BigDecimal("3.00"), Dinheiro.centavos(new BigDecimal("2.995")));
        assertEquals(new BigDecimal("2.98"), Dinheiro.centavos(new BigDecimal("2.985")));
    }
}
