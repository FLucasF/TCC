package br.tcc.checkout.enums;

import java.util.HashMap;
import java.util.Map;
import br.tcc.checkout.exception.CheckoutException;
import br.tcc.checkout.util.MoneyUtil;

public enum Cupom {
	BEMVINDO10 {
		@Override
		public double calcularDesconto(double subtotalProdutos, double frete) {
			return MoneyUtil.round(subtotalProdutos * 0.10);
		}

		@Override
		public boolean isAplicavel(double subtotalProdutos, double frete) {
			return true;
		}
	},
	MENOS50 {
		@Override
		public double calcularDesconto(double subtotalProdutos, double frete) {
			return 50.0;
		}

		@Override
		public boolean isAplicavel(double subtotalProdutos, double frete) {
			return subtotalProdutos >= 300.0;
		}
	},
	FRETEGRATIS {
		@Override
		public double calcularDesconto(double subtotalProdutos, double frete) {
			return MoneyUtil.round(frete);
		}

		@Override
		public boolean isAplicavel(double subtotalProdutos, double frete) {
			return true;
		}
	},
	LEVE3PAGUE2 {
		@Override
		public double calcularDesconto(double subtotalProdutos, double frete) {
			// Este cupom é aplicado na lista de itens, não aqui
			return 0.0;
		}

		@Override
		public boolean isAplicavel(double subtotalProdutos, double frete) {
			return true;
		}
	};

	public abstract double calcularDesconto(double subtotalProdutos, double frete);

	public abstract boolean isAplicavel(double subtotalProdutos, double frete);

	public static Cupom from(String valor) {
		if (valor == null || valor.isEmpty()) {
			return null;
		}
		try {
			return Cupom.valueOf(valor);
		} catch (IllegalArgumentException e) {
			throw new CheckoutException("CUPOM_INVALIDO");
		}
	}

	private static final Map<String, Cupom> cupons = new HashMap<>();

	static {
		cupons.put("BEMVINDO10", BEMVINDO10);
		cupons.put("MENOS50", MENOS50);
		cupons.put("FRETEGRATIS", FRETEGRATIS);
		cupons.put("LEVE3PAGUE2", LEVE3PAGUE2);
	}

	public static boolean existe(String codigo) {
		if (codigo == null || codigo.isEmpty()) {
			return false;
		}
		return cupons.containsKey(codigo);
	}
}
