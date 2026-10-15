package br.tcc.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Regras de arredondamento monetário: centavos, meio para o par. */
public final class Dinheiro {

	public static final int CASAS = 2;
	public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
	/** Precisão usada nos cálculos intermediários (juros), antes do arredondamento final. */
	public static final MathContext CALCULO = MathContext.DECIMAL128;

	public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

	private Dinheiro() {
	}

	public static BigDecimal arredondar(BigDecimal valor) {
		return valor.setScale(CASAS, MODO);
	}
}
