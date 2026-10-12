package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.ResumoService;
import com.loja.checkout.service.ValidacaoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestController
@RequestMapping("/checkout")
public class ResumoController {

    private final ResumoService resumoService;

    public ResumoController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<ResumoResponse> calcularResumo(@RequestBody PedidoRequest pedido) {
        ResumoResponse resumo = resumoService.calcular(pedido);
        return ResponseEntity.ok(resumo);
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<ErroResponse> handleValidacaoException(ValidacaoException ex) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(ex.getCodigo().name()));
    }
}
