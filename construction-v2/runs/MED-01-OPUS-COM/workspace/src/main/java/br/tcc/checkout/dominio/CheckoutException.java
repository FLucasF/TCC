package br.tcc.checkout.dominio;

/** Falha de validação do pedido, devolvida ao site como código de erro. */
public class CheckoutException extends RuntimeException {

	private final String codigo;

	public CheckoutException(String codigo) {
		super(codigo);
		this.codigo = codigo;
	}

	public String codigo() {
		return codigo;
	}
}
