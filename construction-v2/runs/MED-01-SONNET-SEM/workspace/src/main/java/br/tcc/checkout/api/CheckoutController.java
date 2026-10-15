package br.tcc.checkout.api;

import br.tcc.checkout.api.dto.CheckoutRequest;
import br.tcc.checkout.api.dto.ResumoCompraResponse;
import br.tcc.checkout.service.CheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

	private final CheckoutService checkoutService;

	public CheckoutController(CheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping("/resumo")
	public ResumoCompraResponse resumo(@RequestBody CheckoutRequest request) {
		return checkoutService.calcularResumo(request);
	}
}
