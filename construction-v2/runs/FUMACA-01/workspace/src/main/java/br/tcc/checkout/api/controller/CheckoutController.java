package br.tcc.checkout.api.controller;

import br.tcc.checkout.api.dto.ErroResponse;
import br.tcc.checkout.api.dto.ResumoCheckoutRequest;
import br.tcc.checkout.api.dto.ResumoCheckoutResponse;
import br.tcc.checkout.service.ResumoCheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ResumoCheckoutService service;

    public CheckoutController(ResumoCheckoutService service) {
        this.service = service;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody ResumoCheckoutRequest request) {
        try {
            ResumoCheckoutResponse response = service.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (ResumoCheckoutService.ErroValidacao e) {
            return ResponseEntity.badRequest()
                .body(new ErroResponse(e.getCodigo()));
        }
    }
}
