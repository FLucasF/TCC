package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DinheiroTest {

    @ParameterizedTest
    @CsvSource({
            "2.995, 3.00",
            "2.985, 2.98",
            "20.485, 20.48",
            "20.6035, 20.60",
            "7.996, 8.00",
            "2.991, 2.99",
            "1.005, 1.00",
            "1.015, 1.02"
    })
    void arredondaMeioParaOPar(String valor, String esperado) {
        assertThat(Dinheiro.arredondar(new BigDecimal(valor))).isEqualByComparingTo(esperado);
        assertThat(Dinheiro.arredondar(new BigDecimal(valor)).scale()).isEqualTo(2);
    }

    @Test
    void calculaPercentualArredondado() {
        assertThat(Dinheiro.percentual(new BigDecimal("409.70"), new BigDecimal("2.5")))
                .isEqualByComparingTo("10.24");
        assertThat(Dinheiro.percentual(new BigDecimal("409.70"), new BigDecimal("5")))
                .isEqualByComparingTo("20.48");
    }
}
