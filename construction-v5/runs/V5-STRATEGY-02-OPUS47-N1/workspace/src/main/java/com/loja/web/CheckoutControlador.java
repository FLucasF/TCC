package com.loja.web;

import com.loja.dominio.CalculadoraResumo;
import com.loja.dominio.ErroCheckout;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckoutControlador {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody PedidoDTO pedido) {
        return ResponseEntity.ok(calculadora.calcular(pedido));
    }

    @ExceptionHandler(ErroCheckout.class)
    public ResponseEntity<?> tratarErroCheckout(ErroCheckout e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.codigo()));
    }
}
