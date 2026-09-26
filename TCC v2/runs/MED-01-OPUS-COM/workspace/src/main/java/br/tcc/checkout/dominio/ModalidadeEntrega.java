package br.tcc.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * As opções de entrega. Cada opção traz o seu jeito de cobrar o frete, o seu
 * prazo e as suas limitações; uma opção nova entra como uma constante nova.
 */
public enum ModalidadeEntrega {

	ECONOMICA(7) {
		@Override
		public BigDecimal frete(Pedido pedido) {
			return fixoMaisPorKg(new BigDecimal("12.00"), new BigDecimal("2.00"), pedido);
		}
	},

	EXPRESSA(2) {
		@Override
		public BigDecimal frete(Pedido pedido) {
			return fixoMaisPorKg(new BigDecimal("25.00"), new BigDecimal("4.50"), pedido);
		}
	},

	RETIRADA_LOJA(1) {
		@Override
		public BigDecimal frete(Pedido pedido) {
			return Dinheiro.ZERO;
		}
	},

	MOTOBOY(0) {
		private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

		@Override
		public BigDecimal frete(Pedido pedido) {
			return Dinheiro.centavos(new BigDecimal("18.00"));
		}

		@Override
		public boolean atende(Pedido pedido) {
			return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
		}
	};

	private final int prazoDias;

	ModalidadeEntrega(int prazoDias) {
		this.prazoDias = prazoDias;
	}

	public static Optional<ModalidadeEntrega> porCodigo(String codigo) {
		if (codigo == null) {
			return Optional.empty();
		}
		return java.util.Arrays.stream(values())
				.filter(modalidade -> modalidade.name().equals(codigo))
				.findFirst();
	}

	public abstract BigDecimal frete(Pedido pedido);

	public boolean atende(Pedido pedido) {
		return true;
	}

	public int prazoDias() {
		return prazoDias;
	}

	static BigDecimal fixoMaisPorKg(BigDecimal fixo, BigDecimal porKg, Pedido pedido) {
		return Dinheiro.centavos(fixo.add(porKg.multiply(pedido.pesoKg())));
	}
}
