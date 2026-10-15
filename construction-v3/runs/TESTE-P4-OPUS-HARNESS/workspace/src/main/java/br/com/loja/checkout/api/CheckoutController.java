package br.com.loja.checkout.api;

import br.com.loja.checkout.CalculadoraResumo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/checkout", produces = MediaType.APPLICATION_JSON_VALUE)
class CheckoutController {

    private final CalculadoraResumo calculadora;

    CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping(path = "/resumo", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResumoResponse resumo(@RequestBody ResumoRequest requisicao) {
        return calculadora.calcular(requisicao);
    }
}
