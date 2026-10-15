package br.tcc.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArredondamentoUtil {

	public static Double arredondarParaCentavos(Double valor) {
		if (valor == null) {
			return 0.0;
		}
		BigDecimal bd = new BigDecimal(valor.toString());
		bd = bd.setScale(2, RoundingMode.HALF_EVEN);
		return bd.doubleValue();
	}

	public static Double arredondarParaCentavos(BigDecimal valor) {
		if (valor == null) {
			return 0.0;
		}
		return valor.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
	}
}
