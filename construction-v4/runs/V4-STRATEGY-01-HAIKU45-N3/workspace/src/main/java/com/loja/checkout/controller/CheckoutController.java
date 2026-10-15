package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.CalculadorResumo;
import com.loja.checkout.validation.ValidadorPedido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CalculadorResumo calculador;

    @Autowired
    private ValidadorPedido validador;

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody PedidoRequest request) {
        String erro = validador.validar(request);
        if (erro != null) {
            return ResponseEntity.badRequest().body(new ErroResponse(erro));
        }

        ResumoResponse resumo = calculador.calcular(request);
        return ResponseEntity.ok(resumo);
    }
}
