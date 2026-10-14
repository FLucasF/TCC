package br.com.loja.pedidos.checkout;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CalculadoraCheckout calculadora;

    public CheckoutController(CalculadoraCheckout calculadora) {
        this.calculadora = calculadora;
    }

    /** Calcula o resumo da compra, sem registrar nada. */
    @PostMapping("/checkout/resumo")
    public Resumo resumo(@RequestBody(required = false) Object corpo) {
        return calculadora.calcular(DadosCompra.ler(corpo)).resumo();
    }
}
