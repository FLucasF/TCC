package com.loja.checkout.api;

import com.loja.checkout.CalculadoraResumo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return calculadora.calcular(request);
    }

    @ExceptionHandler(PedidoRecusado.class)
    ResponseEntity<ErroResponse> recusado(PedidoRecusado recusado) {
        return ResponseEntity.badRequest().body(new ErroResponse(recusado.codigo()));
    }

    /** JSON que não dá nem para ler é pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException ignorada) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
