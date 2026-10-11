package com.loja.checkout.web;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
class CheckoutController {
    private final CheckoutService service;

    CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/resumo")
    ResponseEntity<ResumoResponse> resumo(@RequestBody PedidoRequest request) {
        return ResponseEntity.ok(service.calcular(request));
    }

    @ExceptionHandler(CheckoutException.class)
    ResponseEntity<ErroResponse> handleCheckout(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(ex.getCodigo()));
    }
}
