package br.tcc.checkout.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.tcc.checkout.dominio.CheckoutException;
import br.tcc.checkout.dominio.ErroCheckout;

/** Traduz os erros de negócio para HTTP 400 com o corpo combinado com o site. */
@RestControllerAdvice
public class CheckoutErrorHandler {

	@ExceptionHandler(CheckoutException.class)
	public ResponseEntity<ErroResponse> erroDeNegocio(CheckoutException excecao) {
		return badRequest(excecao.getErro());
	}

	/** JSON malformado ou com tipos errados: o pedido não dá nem para ser lido. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
		return badRequest(ErroCheckout.PEDIDO_INVALIDO);
	}

	private ResponseEntity<ErroResponse> badRequest(ErroCheckout erro) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(erro.name()));
	}
}
