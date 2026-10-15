package com.loja.checkout.web;

import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.resumo.ResumoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckoutController {

    private final ResumoService resumoService;

    public CheckoutController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody PedidoRequest pedido) {
        return resumoService.calcular(pedido);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> tratarErro(CheckoutException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("erro", e.getCodigo()));
    }
}
