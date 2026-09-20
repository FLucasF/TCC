package br.tcc.checkout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.tcc.checkout.dto.ErroResponse;
import br.tcc.checkout.dto.ResumoCheckoutRequest;
import br.tcc.checkout.dto.ResumoCheckoutResponse;
import br.tcc.checkout.exception.CheckoutException;
import br.tcc.checkout.service.CheckoutService;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody ResumoCheckoutRequest request) {
        try {
            ResumoCheckoutResponse resposta = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(resposta);
        } catch (CheckoutException e) {
            ErroResponse erro = new ErroResponse(e.getCodigoErro());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
        }
    }
}
