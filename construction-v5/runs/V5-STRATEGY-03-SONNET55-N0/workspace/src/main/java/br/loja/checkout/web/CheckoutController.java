package br.loja.checkout.web;

import br.loja.checkout.ResumoRequest;
import br.loja.checkout.ResumoResponse;
import br.loja.checkout.ResumoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CheckoutController {
    private final ResumoService service;

    CheckoutController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return service.calcular(request);
    }
}
