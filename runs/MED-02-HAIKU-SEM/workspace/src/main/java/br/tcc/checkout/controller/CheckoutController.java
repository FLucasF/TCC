package br.tcc.checkout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.tcc.checkout.model.RequisicaoResumo;
import br.tcc.checkout.model.RespostaErro;
import br.tcc.checkout.model.RespostaResumo;
import br.tcc.checkout.service.ResumoCheckoutService;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

	@Autowired
	private ResumoCheckoutService resumoCheckoutService;

	@PostMapping("/resumo")
	public ResponseEntity<?> resumo(@RequestBody RequisicaoResumo requisicao) {
		String erro = resumoCheckoutService.validarRequisicao(requisicao);

		if (erro != null) {
			return ResponseEntity.badRequest().body(new RespostaErro(erro));
		}

		RespostaResumo resumo = resumoCheckoutService.calcularResumo(requisicao);
		return ResponseEntity.ok(resumo);
	}
}
