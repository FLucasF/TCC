package br.com.loja.checkout.api;

import br.com.loja.checkout.CalculadoraResumo;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.Resumo;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public Map<String, String> erro(CheckoutException e) {
        return Map.of("erro", e.codigo());
    }
}
