package br.tcc.checkout.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.tcc.checkout.aplicacao.CalculadoraResumo;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

	private final CalculadoraResumo calculadora;

	public CheckoutController(CalculadoraResumo calculadora) {
		this.calculadora = calculadora;
	}

	@PostMapping(path = "/resumo", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest request) {
		return calculadora.calcular(request);
	}
}
