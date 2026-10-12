package br.com.loja.checkout.api;

import br.com.loja.checkout.CheckoutService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping(path = "/resumo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest request) {
        return checkoutService.calcularResumo(request);
    }
}
