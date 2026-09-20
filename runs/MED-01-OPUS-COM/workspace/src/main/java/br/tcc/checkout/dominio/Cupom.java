package br.tcc.checkout.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Os cupons da casa. Cada promoção traz a sua condição e a sua conta de
 * desconto; uma promoção nova entra como uma constante nova.
 */
public enum Cupom {

	BEMVINDO10 {
		@Override
		public BigDecimal desconto(ContextoCupom contexto) {
			return Dinheiro.centavos(contexto.subtotalProdutos().multiply(new BigDecimal("0.10")));
		}
	},

	MENOS50 {
		private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

		@Override
		public boolean aplicavel(ContextoCupom contexto) {
			return contexto.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
		}

		@Override
		public BigDecimal desconto(ContextoCupom contexto) {
			return Dinheiro.centavos(new BigDecimal("50.00"));
		}
	},

	FRETEGRATIS {
		@Override
		public BigDecimal desconto(ContextoCupom contexto) {
			return Dinheiro.centavos(contexto.frete());
		}
	},

	LEVE3PAGUE2 {
		@Override
		public BigDecimal desconto(ContextoCupom contexto) {
			return Dinheiro.centavos(contexto.pedido().itens().stream()
					.map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
					.reduce(BigDecimal.ZERO, BigDecimal::add));
		}
	};

	public static Optional<Cupom> porCodigo(String codigo) {
		if (codigo == null) {
			return Optional.empty();
		}
		return Arrays.stream(values())
				.filter(cupom -> cupom.name().equals(codigo))
				.findFirst();
	}

	public abstract BigDecimal desconto(ContextoCupom contexto);

	public boolean aplicavel(ContextoCupom contexto) {
		return true;
	}
}
