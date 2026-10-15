package br.tcc.checkout.enums;

import br.tcc.checkout.exception.CheckoutException;

public enum ModalidadeEntrega {
	ECONOMICA(12.0, 2.0, 7) {
		@Override
		public double calcularFrete(double pesoTotal) {
			return 12.0 + (2.0 * pesoTotal);
		}

		@Override
		public int getPrazo() {
			return 7;
		}
	},
	EXPRESSA(25.0, 4.5, 2) {
		@Override
		public double calcularFrete(double pesoTotal) {
			return 25.0 + (4.5 * pesoTotal);
		}

		@Override
		public int getPrazo() {
			return 2;
		}
	},
	RETIRADA_LOJA(0.0, 0.0, 1) {
		@Override
		public double calcularFrete(double pesoTotal) {
			return 0.0;
		}

		@Override
		public int getPrazo() {
			return 1;
		}
	},
	MOTOBOY(18.0, 0.0, 0) {
		@Override
		public double calcularFrete(double pesoTotal) {
			return 18.0;
		}

		@Override
		public int getPrazo() {
			return 0;
		}
	};

	private final double tarifa;
	private final double porKg;
	private final int prazo;

	ModalidadeEntrega(double tarifa, double porKg, int prazo) {
		this.tarifa = tarifa;
		this.porKg = porKg;
		this.prazo = prazo;
	}

	public abstract double calcularFrete(double pesoTotal);

	public abstract int getPrazo();

	public static ModalidadeEntrega from(String valor) {
		try {
			return ModalidadeEntrega.valueOf(valor);
		} catch (IllegalArgumentException e) {
			throw new CheckoutException("MODALIDADE_INVALIDA");
		}
	}

	public boolean isDisponivel(double pesoTotal) {
		if (this == MOTOBOY) {
			return pesoTotal <= 5.0;
		}
		return true;
	}
}
