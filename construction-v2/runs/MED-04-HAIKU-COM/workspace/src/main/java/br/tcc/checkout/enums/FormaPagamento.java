package br.tcc.checkout.enums;

import br.tcc.checkout.exception.CheckoutException;
import br.tcc.checkout.util.MoneyUtil;

public enum FormaPagamento {
	PIX {
		@Override
		public double calcularAjuste(double total, int parcelas) {
			double desconto = MoneyUtil.round(total * 0.05);
			return -desconto;
		}

		@Override
		public boolean isParcelaValida(int parcelas) {
			return parcelas == 1;
		}

		@Override
		public boolean isDisponivel(double total) {
			return true;
		}

		@Override
		public double calcularValorParcela(double totalSemAjuste, int parcelas) {
			double desconto = MoneyUtil.round(totalSemAjuste * 0.05);
			return MoneyUtil.round((totalSemAjuste - desconto) / parcelas);
		}
	},
	CARTAO {
		@Override
		public double calcularAjuste(double total, int parcelas) {
			if (parcelas <= 3) {
				return 0.0;
			}
			double taxaMensal = 0.0199;
			double numerador = total * taxaMensal;
			double denominador = 1 - Math.pow(1 + taxaMensal, -parcelas);
			double parcela = numerador / denominador;
			parcela = MoneyUtil.round(parcela);
			double totalComJuros = parcela * parcelas;
			return MoneyUtil.round(totalComJuros - total);
		}

		@Override
		public boolean isParcelaValida(int parcelas) {
			return parcelas >= 1 && parcelas <= 12;
		}

		@Override
		public boolean isDisponivel(double total) {
			return true;
		}

		@Override
		public double calcularValorParcela(double totalSemAjuste, int parcelas) {
			if (parcelas <= 3) {
				return MoneyUtil.round(totalSemAjuste / parcelas);
			}
			double taxaMensal = 0.0199;
			double numerador = totalSemAjuste * taxaMensal;
			double denominador = 1 - Math.pow(1 + taxaMensal, -parcelas);
			double parcela = numerador / denominador;
			return MoneyUtil.round(parcela);
		}
	},
	BOLETO {
		@Override
		public double calcularAjuste(double total, int parcelas) {
			return 3.49;
		}

		@Override
		public boolean isParcelaValida(int parcelas) {
			return parcelas == 1;
		}

		@Override
		public boolean isDisponivel(double total) {
			return total <= 1000.0;
		}

		@Override
		public double calcularValorParcela(double totalSemAjuste, int parcelas) {
			return MoneyUtil.round((totalSemAjuste + 3.49) / parcelas);
		}
	};

	public abstract double calcularAjuste(double total, int parcelas);

	public abstract boolean isParcelaValida(int parcelas);

	public abstract boolean isDisponivel(double total);

	public abstract double calcularValorParcela(double total, int parcelas);

	public static FormaPagamento from(String valor) {
		try {
			return FormaPagamento.valueOf(valor);
		} catch (IllegalArgumentException e) {
			throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
		}
	}
}
