package br.com.loja.checkout.api;

import br.com.loja.checkout.CalculadoraResumo;
import br.com.loja.checkout.Resumo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public Resumo resumo(@RequestBody ResumoRequest request) {
        return calculadora.calcular(request.paraPedido());
    }
}
