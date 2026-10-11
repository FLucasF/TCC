package com.loja.checkout.api;

import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.servico.CheckoutServico;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutServico checkoutServico;

    public CheckoutController(CheckoutServico checkoutServico) {
        this.checkoutServico = checkoutServico;
    }

    @PostMapping("/resumo")
    public ResponseEntity<ResumoResponse> resumo(@RequestBody ResumoRequest request) {
        ResumoResponse resumo = checkoutServico.calcular(request);
        return ResponseEntity.ok(resumo);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> handleCheckoutException(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("erro", ex.getErro().name()));
    }
}
