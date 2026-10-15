package com.loja.checkout.api;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.DadosCompra;
import com.loja.checkout.dominio.ResumoCompra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint que o site chama para montar o resumo da compra. */
@RestController
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompra resumo(@RequestBody DadosCompra dados) {
        return calculadora.calcular(dados);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResposta> pedidoRecusado(CheckoutException excecao) {
        return ResponseEntity.badRequest().body(ErroResposta.de(excecao.codigo()));
    }

    /** JSON que nao da nem para ler (numero no lugar de texto, corpo vazio) e pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErroResposta.de(CodigoErro.PEDIDO_INVALIDO));
    }
}
