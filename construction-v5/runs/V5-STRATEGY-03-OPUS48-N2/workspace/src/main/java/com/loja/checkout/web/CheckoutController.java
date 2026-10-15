package com.loja.checkout.web;

import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.servico.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService servico;

    public CheckoutController(CheckoutService servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return servico.calcular(request);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> tratarRecusa(CheckoutException e) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(e.codigo().name()));
    }
}
