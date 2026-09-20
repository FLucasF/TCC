package br.tcc.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Optional;

/**
 * As formas de pagamento. Cada uma traz o seu parcelamento, as suas limitações
 * e o seu ajuste sobre o total do pedido.
 */
public enum FormaPagamento {

	PIX {
		private static final BigDecimal DESCONTO = new BigDecimal("0.05");

		@Override
		public Cobranca cobranca(ContextoPagamento contexto) {
			BigDecimal desconto = Dinheiro.centavos(contexto.totalPedido().multiply(DESCONTO));
			return aVista(contexto.totalPedido().subtract(desconto));
		}
	},

	CARTAO {
		private static final int PARCELAS_MAXIMAS = 12;
		private static final int PARCELAS_SEM_JUROS = 3;
		private static final BigDecimal TAXA_AO_MES = new BigDecimal("0.0199");

		@Override
		public boolean parcelamentoPermitido(int parcelas) {
			return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
		}

		@Override
		public Cobranca cobranca(ContextoPagamento contexto) {
			BigDecimal total = contexto.totalPedido();
			int parcelas = contexto.parcelas();
			if (parcelas <= PARCELAS_SEM_JUROS) {
				BigDecimal parcela = Dinheiro.centavos(
						total.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
				return new Cobranca(Dinheiro.centavos(total), parcela);
			}
			BigDecimal parcela = Dinheiro.centavos(parcelaPrice(total, TAXA_AO_MES, parcelas));
			return new Cobranca(Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
		}

		/** Tabela Price: total x taxa / (1 - (1 + taxa)^-parcelas). */
		private BigDecimal parcelaPrice(BigDecimal total, BigDecimal taxa, int parcelas) {
			MathContext mc = MathContext.DECIMAL128;
			BigDecimal fator = BigDecimal.ONE.subtract(
					BigDecimal.ONE.divide(BigDecimal.ONE.add(taxa).pow(parcelas, mc), mc));
			return total.multiply(taxa).divide(fator, mc);
		}
	},

	BOLETO {
		private static final BigDecimal TARIFA = new BigDecimal("3.49");
		private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

		@Override
		public boolean atende(ContextoPagamento contexto) {
			return contexto.totalPedido().compareTo(TOTAL_MAXIMO) <= 0;
		}

		@Override
		public Cobranca cobranca(ContextoPagamento contexto) {
			return aVista(contexto.totalPedido().add(TARIFA));
		}
	};

	public static Optional<FormaPagamento> porCodigo(String codigo) {
		if (codigo == null) {
			return Optional.empty();
		}
		return Arrays.stream(values())
				.filter(forma -> forma.name().equals(codigo))
				.findFirst();
	}

	public abstract Cobranca cobranca(ContextoPagamento contexto);

	public boolean parcelamentoPermitido(int parcelas) {
		return parcelas == 1;
	}

	public boolean atende(ContextoPagamento contexto) {
		return true;
	}

	static Cobranca aVista(BigDecimal totalFinal) {
		BigDecimal valor = Dinheiro.centavos(totalFinal);
		return new Cobranca(valor, valor);
	}
}
