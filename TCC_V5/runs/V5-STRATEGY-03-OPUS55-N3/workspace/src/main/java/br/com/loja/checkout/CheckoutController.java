package br.com.loja.checkout;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompra resumo(@RequestBody CheckoutRequest request) {
        return service.resumir(request);
    }

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public Map<String, String> erro(CheckoutException e) {
        return Map.of("erro", e.codigo());
    }
}
