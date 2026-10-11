package com.loja.checkout.web;

import com.loja.checkout.core.CheckoutService;
import com.loja.checkout.core.ResumoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest req) {
        return service.calcular(req);
    }

    @ExceptionHandler(ResumoException.class)
    public ResponseEntity<ErroResponse> pedidoRecusado(ResumoException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse(e.getCodigo()));
    }
}
