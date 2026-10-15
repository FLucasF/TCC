package br.com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DinheiroTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"2.995, 3.00", "2.985, 2.98", "2.005, 2.00", "2.015, 2.02", "2.001, 2.00", "10, 10.00"})
    void arredondaMeioParaOPar(String valor, String esperado) {
        assertThat(Dinheiro.centavos(new BigDecimal(valor))).isEqualByComparingTo(esperado);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"2.995, 3.00", "2.985, 2.98"})
    void sempreComDuasCasas(String valor, String esperado) {
        assertThat(Dinheiro.centavos(new BigDecimal(valor)).scale()).isEqualTo(2);
    }
}
