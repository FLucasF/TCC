package com.loja.checkout.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DinheiroTest {

    @ParameterizedTest
    @CsvSource({
            "2.995, 3.00",
            "2.985, 2.98",
            "20.485, 20.48",
            "7.996, 8.00",
            "10.2425, 10.24"
    })
    @DisplayName("Arredonda para centavos, meio para o par")
    void arredondaMeioParaOPar(String valor, String esperado) {
        assertThat(Dinheiro.centavos(new BigDecimal(valor))).isEqualTo(new BigDecimal(esperado));
    }
}
