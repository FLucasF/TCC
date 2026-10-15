package br.tcc.checkout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaErro;
import br.tcc.checkout.dto.RespostaResumo;
import br.tcc.checkout.exception.ErroCheckout;
import br.tcc.checkout.service.CheckoutService;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody RequisicaoResumo requisicao) {
        try {
            RespostaResumo resposta = checkoutService.calcularResumo(requisicao);
            return ResponseEntity.ok(resposta);
        } catch (ErroCheckout e) {
            RespostaErro erro = new RespostaErro(e.getCodigo());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
        }
    }
}
