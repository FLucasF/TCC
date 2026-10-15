package br.com.loja.checkout.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ResumoCheckoutService servico;

    public CheckoutController(ResumoCheckoutService servico) {
        this.servico = servico;
    }

    @PostMapping(path = "/resumo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoResponse resumo(@RequestBody ResumoRequest requisicao) {
        return servico.calcular(requisicao);
    }
}
