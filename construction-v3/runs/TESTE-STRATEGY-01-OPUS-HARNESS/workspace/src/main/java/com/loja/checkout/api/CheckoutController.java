package com.loja.checkout.api;

import com.loja.checkout.CalculadoraResumo;
import com.loja.checkout.dominio.CheckoutInvalidoException;
import com.loja.checkout.dominio.ResumoCompra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.loja.checkout.dominio.ErroCheckout.PEDIDO_INVALIDO;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResumoCompra resumo(@RequestBody(required = false) ResumoRequest pedido) {
        return calculadora.calcular(pedido);
    }

    @ExceptionHandler(CheckoutInvalidoException.class)
    public ResponseEntity<ErroResponse> invalido(CheckoutInvalidoException e) {
        return ResponseEntity.badRequest().body(ErroResponse.de(e.erro()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErroResponse.de(PEDIDO_INVALIDO));
    }
}
