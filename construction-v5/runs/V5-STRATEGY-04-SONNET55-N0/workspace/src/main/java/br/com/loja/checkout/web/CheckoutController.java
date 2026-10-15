package br.com.loja.checkout.web;

import br.com.loja.checkout.dominio.Resumo;
import br.com.loja.checkout.servico.ResumoRequest;
import br.com.loja.checkout.servico.ResumoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoService service;

    public CheckoutController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public Resumo resumo(@RequestBody(required = false) ResumoRequest request) {
        return service.calcular(request);
    }
}
