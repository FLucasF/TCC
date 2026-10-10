package com.loja.controller;

import com.loja.exception.CheckoutException;
import com.loja.model.ErroResponse;
import com.loja.model.PedidoRequest;
import com.loja.model.ResumoResponse;
import com.loja.service.ResumoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    private final ResumoService resumoService;

    public CheckoutController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<ResumoResponse> calcularResumo(@RequestBody PedidoRequest pedido) {
        ResumoResponse resumo = resumoService.calcularResumo(pedido);
        return ResponseEntity.ok(resumo);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> handleCheckoutException(CheckoutException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(ex.getCodigo()));
    }
}
