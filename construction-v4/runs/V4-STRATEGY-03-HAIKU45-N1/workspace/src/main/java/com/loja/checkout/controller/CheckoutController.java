package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.ResumoCheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    public ResponseEntity<ResumoResponse> calcularResumo(@RequestBody PedidoRequest request) {
        ResumoResponse resumo = checkoutService.calcularResumo(request);
        return ResponseEntity.ok(resumo);
    }

    @ExceptionHandler(ResumoCheckoutException.class)
    public ResponseEntity<ErroResponse> handleResumoCheckoutException(ResumoCheckoutException e) {
        ErroResponse erro = new ErroResponse(e.getCodigoErro());
        return ResponseEntity.badRequest().body(erro);
    }
}
