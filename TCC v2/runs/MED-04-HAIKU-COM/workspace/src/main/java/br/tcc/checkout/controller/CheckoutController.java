package br.tcc.checkout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.tcc.checkout.dto.CheckoutRequest;
import br.tcc.checkout.dto.CheckoutResponse;
import br.tcc.checkout.dto.ErrorResponse;
import br.tcc.checkout.exception.CheckoutException;
import br.tcc.checkout.service.CheckoutService;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

	@Autowired
	private CheckoutService checkoutService;

	@PostMapping("/resumo")
	public ResponseEntity<CheckoutResponse> resumo(@RequestBody CheckoutRequest request) {
		CheckoutResponse response = checkoutService.calcularResumo(request);
		return ResponseEntity.ok(response);
	}

	@ExceptionHandler(CheckoutException.class)
	public ResponseEntity<ErrorResponse> handleCheckoutException(CheckoutException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErrorResponse(e.getCodigoErro()));
	}
}
