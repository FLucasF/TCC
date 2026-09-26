package br.tcc.checkout.exception;

public class CheckoutException extends RuntimeException {

	private final CheckoutErrorCode codigo;

	public CheckoutException(CheckoutErrorCode codigo) {
		super(codigo.name());
		this.codigo = codigo;
	}

	public CheckoutErrorCode getCodigo() {
		return codigo;
	}
}
