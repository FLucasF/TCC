package br.tcc.checkout.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DinheiroTest {

	@ParameterizedTest
	@CsvSource({
			"2.995, 3.00",
			"2.985, 2.98",
			"2.005, 2.00",
			"2.015, 2.02",
			"20.0915, 20.09",
			"1.0, 1.00"
	})
	void arredondaEmCentavosMeioParaOPar(String valor, String esperado) {
		assertThat(Dinheiro.arredondar(new BigDecimal(valor))).isEqualByComparingTo(esperado);
	}

	@Test
	void mantemDuasCasasNaEscala() {
		assertThat(Dinheiro.arredondar(new BigDecimal("1.5")).scale()).isEqualTo(2);
		assertThat(Dinheiro.ZERO).hasToString("0.00");
	}
}
