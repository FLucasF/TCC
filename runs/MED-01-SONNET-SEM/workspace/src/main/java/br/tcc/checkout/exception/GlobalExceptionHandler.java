package br.tcc.checkout.exception;

import br.tcc.checkout.api.dto.ErroResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CheckoutException.class)
	public ResponseEntity<ErroResponse> tratarCheckoutException(CheckoutException ex) {
		return ResponseEntity.badRequest().body(new ErroResponse(ex.getCodigo().name()));
	}
}
