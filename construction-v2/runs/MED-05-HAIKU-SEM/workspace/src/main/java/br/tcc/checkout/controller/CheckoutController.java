package br.tcc.checkout.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaErro;
import br.tcc.checkout.dto.RespostaResumo;
import br.tcc.checkout.service.CheckoutService;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

	private final CheckoutService checkoutService;

	public CheckoutController(CheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping("/resumo")
	public ResponseEntity<?> resumo(@RequestBody RequisicaoResumo req) {
		String validacaoErro = checkoutService.validarRequisicao(req);
		if (validacaoErro != null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RespostaErro(validacaoErro));
		}

		String disponibilidadeErro = checkoutService.validarDisponibilidade(req);
		if (disponibilidadeErro != null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RespostaErro(disponibilidadeErro));
		}

		RespostaResumo resumo = checkoutService.calcularResumo(req);
		return ResponseEntity.ok(resumo);
	}
}
