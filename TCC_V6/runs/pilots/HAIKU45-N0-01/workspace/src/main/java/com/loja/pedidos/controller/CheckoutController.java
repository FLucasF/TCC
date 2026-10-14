package com.loja.pedidos.controller;

import com.loja.pedidos.dto.DadosCompra;
import com.loja.pedidos.dto.Resumo;
import com.loja.pedidos.dto.RespostaErro;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.service.CalculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {
    private final CalculoService calculoService;

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody DadosCompra dados) {
        try {
            var resumo = calculoService.calcularResumo(dados);
            return ResponseEntity.ok(resumo);
        } catch (ErroNegocio e) {
            return ResponseEntity
                .status(e.getStatus())
                .body(new RespostaErro(e.getCodigo()));
        }
    }
}
