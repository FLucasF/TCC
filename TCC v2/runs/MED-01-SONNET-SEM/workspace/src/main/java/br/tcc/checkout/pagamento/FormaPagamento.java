package br.tcc.checkout.pagamento;

import br.tcc.checkout.exception.CheckoutErrorCode;
import br.tcc.checkout.exception.CheckoutException;

public enum FormaPagamento {

	PIX,
	CARTAO,
	BOLETO;

	public static FormaPagamento fromCodigo(String codigo) {
		if (codigo != null) {
			for (FormaPagamento forma : values()) {
				if (forma.name().equals(codigo)) {
					return forma;
				}
			}
		}
		throw new CheckoutException(CheckoutErrorCode.FORMA_PAGAMENTO_INVALIDA);
	}
}
