package br.tcc.checkout.exception;

public class CheckoutException extends RuntimeException {
	private final String codigoErro;

	public CheckoutException(String codigoErro) {
		super(codigoErro);
		this.codigoErro = codigoErro;
	}

	public String getCodigoErro() {
		return codigoErro;
	}
}
