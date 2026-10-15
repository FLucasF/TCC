package com.loja.checkout.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.Resultado;
import com.loja.checkout.service.Resultado.Erro;
import com.loja.checkout.service.Resultado.Sucesso;
import com.loja.checkout.service.ResumoService;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    private final ResumoService resumoService;

    public CheckoutController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody PedidoRequest pedido) {
        Resultado resultado = resumoService.calcular(pedido);

        return switch (resultado) {
            case Sucesso sucesso -> ResponseEntity.ok(sucesso.resumo());
            case Erro erro -> ResponseEntity.badRequest().body(new ErroResponse(erro.codigo()));
        };
    }
}
