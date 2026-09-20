package br.tcc.checkout.dominio;

/** Erro de negócio do checkout, traduzido para HTTP 400 com o código correspondente. */
public class CheckoutException extends RuntimeException {

	private final ErroCheckout erro;

	public CheckoutException(ErroCheckout erro) {
		super(erro.name());
		this.erro = erro;
	}

	public ErroCheckout getErro() {
		return erro;
	}
}
