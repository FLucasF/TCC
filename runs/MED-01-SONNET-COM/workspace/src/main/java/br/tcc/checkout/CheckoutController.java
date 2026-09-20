package br.tcc.checkout;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.tcc.checkout.dto.ResumoRequest;
import br.tcc.checkout.dto.ResumoResponse;

@RestController
@RequestMapping("/checkout")
class CheckoutController {

    private final CheckoutService checkoutService;

    CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return checkoutService.calcularResumo(request);
    }
}
