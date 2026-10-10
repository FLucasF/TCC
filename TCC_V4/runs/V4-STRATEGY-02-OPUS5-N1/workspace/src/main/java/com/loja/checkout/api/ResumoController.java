package com.loja.checkout.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loja.checkout.erro.Codigo;
import com.loja.checkout.erro.PedidoRecusado;

/** Onde o site pede o resumo da compra. */
@RestController
@RequestMapping("/checkout")
public class ResumoController {

    private final CalculadoraDeResumo calculadora;

    public ResumoController(CalculadoraDeResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody CompraRequest compra) {
        return calculadora.calcular(compra);
    }

    @ExceptionHandler(PedidoRecusado.class)
    ResponseEntity<ErroResponse> pedidoRecusado(PedidoRecusado recusa) {
        return recusado(recusa.codigo());
    }

    /** Dados que nem dao para ler sao tratados como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> dadosIlegiveis(HttpMessageNotReadableException erro) {
        return recusado(Codigo.PEDIDO_INVALIDO);
    }

    private static ResponseEntity<ErroResponse> recusado(Codigo codigo) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErroResponse(codigo.name()));
    }
}
